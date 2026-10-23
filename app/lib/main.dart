import 'package:flutter/material.dart';

import 'auth_gate.dart';
import 'session/session_controller.dart';
import 'theme.dart';

void main() {
  runApp(ContaPlusApp(session: SessionController.create()));
}

class ContaPlusApp extends StatefulWidget {
  const ContaPlusApp({super.key, required this.session});

  final SessionController session;

  @override
  State<ContaPlusApp> createState() => _ContaPlusAppState();
}

class _ContaPlusAppState extends State<ContaPlusApp> {
  final _navigatorKey = GlobalKey<NavigatorState>();

  @override
  void initState() {
    super.initState();
    widget.session.addListener(_onSessionChanged);
    widget.session.restore();
  }

  @override
  void dispose() {
    widget.session.removeListener(_onSessionChanged);
    super.dispose();
  }

  void _onSessionChanged() {
    // Ao perder a sessão, fecha as telas empilhadas para o login aparecer por cima de tudo.
    if (widget.session.status == SessionStatus.unauthenticated) {
      _navigatorKey.currentState?.popUntil((route) => route.isFirst);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SessionScope(
      session: widget.session,
      child: MaterialApp(
        navigatorKey: _navigatorKey,
        title: 'Conta+',
        debugShowCheckedModeBanner: false,
        theme: buildAppTheme(),
        home: const AuthGate(),
      ),
    );
  }
}
