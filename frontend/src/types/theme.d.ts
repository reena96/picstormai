/**
 * Theme type definitions
 */

export interface ColorScale {
  50: string;
  100: string;
  200: string;
  300: string;
  400: string;
  500: string;
  600: string;
  700: string;
  800: string;
  900: string;
}

export interface Theme {
  colors: {
    primary: ColorScale;
    success: Partial<ColorScale>;
    error: Partial<ColorScale>;
    warning: Partial<ColorScale>;
    info: Partial<ColorScale>;
    gray: ColorScale;
    white: string;
    black: string;
    transparent: string;
    // Semantic colors
    background: string;
    surface: string;
    text: {
      primary: string;
      secondary: string;
      disabled: string;
      inverse: string;
    };
    border: string;
  };
  typography: {
    fontFamily: {
      primary: string;
      mono: string;
    };
    fontSize: {
      xs: number;
      sm: number;
      base: number;
      lg: number;
      xl: number;
      '2xl': number;
      '3xl': number;
      '4xl': number;
      '5xl': number;
      '6xl': number;
    };
    fontWeight: {
      thin: string;
      light: string;
      normal: string;
      medium: string;
      semibold: string;
      bold: string;
      extrabold: string;
    };
    lineHeight: {
      tight: number;
      normal: number;
      relaxed: number;
    };
  };
  spacing: {
    0: number;
    1: number;
    2: number;
    3: number;
    4: number;
    5: number;
    6: number;
    8: number;
    10: number;
    12: number;
    16: number;
    20: number;
  };
  shadows: {
    xs: ShadowStyle;
    sm: ShadowStyle;
    base: ShadowStyle;
    md: ShadowStyle;
    lg: ShadowStyle;
    xl: ShadowStyle;
  };
  borderRadius: {
    none: number;
    sm: number;
    base: number;
    md: number;
    lg: number;
    xl: number;
    full: number;
  };
  animations: {
    duration: {
      instant: number;
      fast: number;
      normal: number;
      slow: number;
      slower: number;
    };
    easing: {
      ease: string;
      easeIn: string;
      easeOut: string;
      easeInOut: string;
      spring: string;
    };
  };
}

export interface ShadowStyle {
  shadowColor: string;
  shadowOffset: { width: number; height: number };
  shadowOpacity: number;
  shadowRadius: number;
  elevation: number;
}

export type ThemeMode = 'light' | 'dark' | 'auto';

export interface ThemeContextValue {
  theme: Theme;
  mode: ThemeMode;
  isDark: boolean;
  toggleTheme: () => void;
  setTheme: (mode: ThemeMode) => void;
}
