import 'package:flutter/material.dart';

import '../core/api_client.dart';
import '../theme.dart';
import 'speech.dart';
import 'voice_models.dart';
import 'voice_service.dart';

/// Folha "O que aconteceu?": o lojista fala (ou digita) e recebe o lançamento
/// interpretado. Devolve null se fechar sem concluir.
Future<Interpretacao?> pedirLancamento(
  BuildContext context, {
  required VoiceService servico,
  Reconhecedor? reconhecedor,
}) {
  return showModalBottomSheet<Interpretacao>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    backgroundColor: AppColors.branco,
    builder: (_) => VoiceSheet(
      servico: servico,
      reconhecedor: reconhecedor ?? ReconhecedorDoAparelho(),
    ),
  );
}

class VoiceSheet extends StatefulWidget {
  const VoiceSheet({
    super.key,
    required this.servico,
    required this.reconhecedor,
  });

  final VoiceService servico;
  final Reconhecedor reconhecedor;

  @override
  State<VoiceSheet> createState() => _VoiceSheetState();
}

class _VoiceSheetState extends State<VoiceSheet> {
  final _texto = TextEditingController();

  bool? _microfoneDisponivel;
  bool _ouvindo = false;
  bool _falou = false;
  bool _interpretando = false;
  String? _aviso;

  @override
  void initState() {
    super.initState();
    _texto.addListener(() => setState(() {}));
    _iniciarMicrofone();
  }

  @override
  void dispose() {
    if (_ouvindo) widget.reconhecedor.parar();
    _texto.dispose();
    super.dispose();
  }

  // Abrir a folha já começa a ouvir: o caminho principal do app é falar.
  Future<void> _iniciarMicrofone() async {
    final disponivel = await widget.reconhecedor.preparar();
    if (!mounted) return;
    setState(() => _microfoneDisponivel = disponivel);
    if (disponivel) _ouvir();
  }

  Future<void> _ouvir() async {
    FocusScope.of(context).unfocus();
    setState(() {
      _ouvindo = true;
      _aviso = null;
    });
    await widget.reconhecedor.ouvir(
      aoReconhecer: (texto, terminou) {
        if (!mounted) return;
        _texto.text = texto;
        _falou = true;
        if (terminou) {
          setState(() => _ouvindo = false);
          if (texto.trim().isNotEmpty) _interpretar();
        }
      },
      aoParar: () {
        if (mounted && _ouvindo) setState(() => _ouvindo = false);
      },
    );
  }

  Future<void> _pararDeOuvir() async {
    await widget.reconhecedor.parar();
    if (mounted) setState(() => _ouvindo = false);
  }

  Future<void> _interpretar() async {
    final frase = _texto.text.trim();
    if (frase.isEmpty || _interpretando) return;
    if (_ouvindo) await _pararDeOuvir();
    if (!mounted) return;
    FocusScope.of(context).unfocus();

    setState(() {
      _interpretando = true;
      _aviso = null;
    });
    try {
      final resultado = await widget.servico.interpretar(
        frase,
        _falou ? FonteLancamento.voz : FonteLancamento.texto,
      );
      if (!mounted) return;
      if (resultado.acionavel) {
        Navigator.of(context).pop(resultado);
        return;
      }
      setState(
        () => _aviso = resultado.mensagem ?? 'Não entendi. Pode repetir?',
      );
    } on ApiException catch (e) {
      if (mounted) setState(() => _aviso = e.message);
    } finally {
      if (mounted) setState(() => _interpretando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;

    return Padding(
      padding: EdgeInsets.fromLTRB(
        24,
        0,
        24,
        24 + MediaQuery.viewInsetsOf(context).bottom,
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            'O que aconteceu?',
            style: texto.titleLarge?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 4),
          Text(
            _ouvindo ? 'Pode falar, estou ouvindo…' : 'Ex.: "vendi 2 cocas no pix", "chegou 10 leites a 4,50", "paguei 180 de luz"',
            textAlign: TextAlign.center,
            style: texto.bodyMedium?.copyWith(color: AppColors.textoSecundario),
          ),
          const SizedBox(height: 20),
          _BotaoMicrofone(
            disponivel: _microfoneDisponivel,
            ouvindo: _ouvindo,
            onPressed: _interpretando
                ? null
                : (_ouvindo ? _pararDeOuvir : _ouvir),
          ),
          if (_microfoneDisponivel == false) ...[
            const SizedBox(height: 8),
            Text(
              'Microfone indisponível. Digite abaixo.',
              style: texto.bodySmall?.copyWith(
                color: AppColors.textoSecundario,
              ),
            ),
          ],
          const SizedBox(height: 20),
          TextField(
            controller: _texto,
            minLines: 1,
            maxLines: 3,
            maxLength: 500,
            textCapitalization: TextCapitalization.sentences,
            textInputAction: TextInputAction.send,
            onTap: () {
              if (_ouvindo) _pararDeOuvir();
            },
            onChanged: (_) => _falou = false,
            onSubmitted: (_) => _interpretar(),
            decoration: const InputDecoration(
              hintText: 'Ou digite aqui',
              counterText: '',
            ),
          ),
          if (_aviso != null) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppColors.alerta.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                _aviso!,
                style: texto.bodyMedium?.copyWith(color: AppColors.alerta),
              ),
            ),
          ],
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: FilledButton(
              onPressed: _texto.text.trim().isEmpty || _interpretando
                  ? null
                  : _interpretar,
              child: _interpretando
                  ? const SizedBox(
                      height: 22,
                      width: 22,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text('Continuar'),
            ),
          ),
        ],
      ),
    );
  }
}

class _BotaoMicrofone extends StatelessWidget {
  const _BotaoMicrofone({
    required this.disponivel,
    required this.ouvindo,
    required this.onPressed,
  });

  final bool? disponivel;
  final bool ouvindo;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final ativo = disponivel == true && onPressed != null;
    return Semantics(
      button: true,
      label: ouvindo ? 'Parar de ouvir' : 'Falar',
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: EdgeInsets.all(ouvindo ? 10 : 0),
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: ouvindo
              ? AppColors.azulBB.withValues(alpha: 0.12)
              : Colors.transparent,
        ),
        child: Material(
          color: ativo ? AppColors.azulBB : AppColors.campo,
          shape: const CircleBorder(),
          child: InkWell(
            customBorder: const CircleBorder(),
            onTap: ativo ? onPressed : null,
            child: SizedBox(
              width: 76,
              height: 76,
              child: Icon(
                ouvindo ? Icons.stop_rounded : Icons.mic_rounded,
                size: 36,
                color: ativo ? AppColors.branco : AppColors.textoSuave,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
