import 'package:flutter/material.dart';

/// Paleta do app: fundo branco + azul Banco do Brasil como cor principal.
class AppColors {
  static const Color azulBB = Color(0xFF1976D2); // azul (primário) — tom mais claro
  static const Color amareloBB = Color(0xFFFAE128); // amarelo BB (acento)
  static const Color branco = Color(0xFFFFFFFF);
  static const Color campo = Color(0xFFF2F4F7); // cinza bem claro dos campos
  static const Color textoSuave = Color(0xFF8A94A6); // ícones/placeholders
  static const Color textoSecundario = Color(0xFF667085);
}

/// Tema global do Conta+. Material 3, superfície branca, primário azul BB,
/// campos "soft" (preenchidos, arredondados, sem borda dura).
ThemeData buildAppTheme() {
  final scheme = ColorScheme.fromSeed(
    seedColor: AppColors.azulBB,
  ).copyWith(primary: AppColors.azulBB);

  OutlineInputBorder borda(Color cor, [double w = 1]) => OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: w == 0 ? BorderSide.none : BorderSide(color: cor, width: w),
      );

  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.branco,
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.branco,
      foregroundColor: AppColors.azulBB,
      elevation: 0,
      scrolledUnderElevation: 0,
      centerTitle: true,
    ),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: AppColors.campo,
      prefixIconColor: AppColors.textoSuave,
      suffixIconColor: AppColors.textoSuave,
      floatingLabelStyle: const TextStyle(color: AppColors.azulBB),
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 18),
      border: borda(AppColors.campo, 0),
      enabledBorder: borda(AppColors.campo, 0),
      focusedBorder: borda(AppColors.azulBB, 1.6),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        backgroundColor: AppColors.azulBB,
        foregroundColor: AppColors.branco,
        minimumSize: const Size.fromHeight(54),
        textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(14),
        ),
      ),
    ),
    textButtonTheme: TextButtonThemeData(
      style: TextButton.styleFrom(
        foregroundColor: AppColors.azulBB,
        textStyle: const TextStyle(fontWeight: FontWeight.w600),
      ),
    ),
  );
}
