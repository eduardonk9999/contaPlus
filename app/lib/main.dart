import 'package:flutter/material.dart';

import 'login_page.dart';
import 'theme.dart';

void main() {
  runApp(const ContaPlusApp());
}

class ContaPlusApp extends StatelessWidget {
  const ContaPlusApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Conta+',
      debugShowCheckedModeBanner: false,
      theme: buildAppTheme(),
      home: const LoginPage(),
    );
  }
}
