import 'package:speech_to_text/speech_to_text.dart';

/// Reconhecimento de fala do aparelho. Interface própria para os testes
/// trocarem o microfone por um falso.
abstract class Reconhecedor {
  /// Pede a permissão do microfone na primeira vez. false = sem fala neste aparelho.
  Future<bool> preparar();

  Future<void> ouvir({
    required void Function(String texto, bool terminou) aoReconhecer,
    required void Function() aoParar,
  });

  Future<void> parar();
}

class ReconhecedorDoAparelho implements Reconhecedor {
  final _speech = SpeechToText();
  void Function()? _aoParar;

  @override
  Future<bool> preparar() async {
    try {
      return await _speech.initialize(
        onStatus: (status) {
          if (status == SpeechToText.doneStatus ||
              status == SpeechToText.notListeningStatus) {
            _aoParar?.call();
          }
        },
        onError: (_) => _aoParar?.call(),
      );
    } catch (_) {
      return false;
    }
  }

  @override
  Future<void> ouvir({
    required void Function(String texto, bool terminou) aoReconhecer,
    required void Function() aoParar,
  }) async {
    _aoParar = aoParar;
    await _speech.listen(
      onResult: (resultado) =>
          aoReconhecer(resultado.recognizedWords, resultado.finalResult),
      listenOptions: SpeechListenOptions(
        localeId: 'pt_BR',
        listenMode: ListenMode.dictation,
        partialResults: true,
        listenFor: const Duration(seconds: 30),
        pauseFor: const Duration(seconds: 3),
      ),
    );
  }

  @override
  Future<void> parar() => _speech.stop();
}
