package org.tasks.themes;

import static org.tasks.kmp.org.tasks.themes.ColorProvider.WHITE;
import static org.tasks.themes.ColorUtilsKt.calculateContrast;

import android.app.Activity;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.core.os.ParcelCompat;

import org.tasks.R;
import org.tasks.dialogs.ColorPalettePicker.Pickable;

public class ThemeColor implements Pickable {

  public static final int[] ICONS =
      new int[] {
        org.tasks.kmp.R.mipmap.ic_launcher_blue_grey,
        org.tasks.kmp.R.mipmap.ic_launcher_dark_grey,
        org.tasks.kmp.R.mipmap.ic_launcher_red,
        org.tasks.kmp.R.mipmap.ic_launcher_pink,
        org.tasks.kmp.R.mipmap.ic_launcher_purple,
        org.tasks.kmp.R.mipmap.ic_launcher_deep_purple,
        org.tasks.kmp.R.mipmap.ic_launcher_indigo,
        org.tasks.kmp.R.mipmap.ic_launcher_blue,
        org.tasks.kmp.R.mipmap.ic_launcher_light_blue,
        org.tasks.kmp.R.mipmap.ic_launcher_cyan,
        org.tasks.kmp.R.mipmap.ic_launcher_teal,
        org.tasks.kmp.R.mipmap.ic_launcher_green,
        org.tasks.kmp.R.mipmap.ic_launcher_light_green,
        org.tasks.kmp.R.mipmap.ic_launcher_lime,
        org.tasks.kmp.R.mipmap.ic_launcher_yellow,
        org.tasks.kmp.R.mipmap.ic_launcher_amber,
        org.tasks.kmp.R.mipmap.ic_launcher_orange,
        org.tasks.kmp.R.mipmap.ic_launcher_deep_orange,
        org.tasks.kmp.R.mipmap.ic_launcher_brown,
        org.tasks.kmp.R.mipmap.ic_launcher_grey
      };

  public static final String[] LAUNCHERS =
      new String[] {
        ".BlueGrey",
        ".DarkGrey",
        ".Red",
        ".Pink",
        ".Purple",
        ".DeepPurple",
        ".Indigo",
        "",
        ".LightBlue",
        ".Cyan",
        ".Teal",
        ".Green",
        ".LightGreen",
        ".Lime",
        ".Yellow",
        ".Amber",
        ".Orange",
        ".DeepOrange",
        ".Brown",
        ".Grey"
      };

  public static final int[] COLORS =
      new int[] {
              org.tasks.kmp.R.color.tomato,
              org.tasks.kmp.R.color.red_500,
              org.tasks.kmp.R.color.deep_orange_500,
              org.tasks.kmp.R.color.tangerine,
              org.tasks.kmp.R.color.pumpkin,
              org.tasks.kmp.R.color.orange_500,
              org.tasks.kmp.R.color.mango,
              org.tasks.kmp.R.color.banana,
              org.tasks.kmp.R.color.amber_500,
              org.tasks.kmp.R.color.citron,
              org.tasks.kmp.R.color.yellow_500,
              org.tasks.kmp.R.color.lime_500,
              org.tasks.kmp.R.color.avocado,
              org.tasks.kmp.R.color.light_green_500,
              org.tasks.kmp.R.color.pistachio,
              org.tasks.kmp.R.color.green_500,
              org.tasks.kmp.R.color.basil,
              org.tasks.kmp.R.color.teal_500,
              org.tasks.kmp.R.color.sage,
              org.tasks.kmp.R.color.cyan_500,
              org.tasks.kmp.R.color.light_blue_500,
              org.tasks.kmp.R.color.peacock,
              org.tasks.kmp.R.color.blue_500,
              org.tasks.kmp.R.color.cobalt,
              org.tasks.kmp.R.color.indigo_500,
              org.tasks.kmp.R.color.lavender,
              org.tasks.kmp.R.color.wisteria,
              org.tasks.kmp.R.color.amethyst,
              org.tasks.kmp.R.color.deep_purple_500,
              org.tasks.kmp.R.color.grape,
              org.tasks.kmp.R.color.purple_500,
              org.tasks.kmp.R.color.radicchio,
              org.tasks.kmp.R.color.pink_500,
              org.tasks.kmp.R.color.cherry_blossom,
              org.tasks.kmp.R.color.flamingo,
              org.tasks.kmp.R.color.brown_500,
              org.tasks.kmp.R.color.graphite,
              org.tasks.kmp.R.color.birch,
              org.tasks.kmp.R.color.grey_500,
              org.tasks.kmp.R.color.blue_grey_500,
              R.color.white_100,
      };

  public static final int[] LAUNCHER_COLORS =
      new int[] {
        org.tasks.kmp.R.color.blue_grey_500,
        org.tasks.kmp.R.color.grey_900,
        org.tasks.kmp.R.color.red_500,
        org.tasks.kmp.R.color.pink_500,
        org.tasks.kmp.R.color.purple_500,
        org.tasks.kmp.R.color.deep_purple_500,
        org.tasks.kmp.R.color.indigo_500,
        org.tasks.kmp.R.color.blue_500,
        org.tasks.kmp.R.color.light_blue_500,
        org.tasks.kmp.R.color.cyan_500,
        org.tasks.kmp.R.color.teal_500,
        org.tasks.kmp.R.color.green_500,
        org.tasks.kmp.R.color.light_green_500,
        org.tasks.kmp.R.color.lime_500,
        org.tasks.kmp.R.color.yellow_500,
        org.tasks.kmp.R.color.amber_500,
        org.tasks.kmp.R.color.orange_500,
        org.tasks.kmp.R.color.deep_orange_500,
        org.tasks.kmp.R.color.brown_500,
        org.tasks.kmp.R.color.grey_500
      };

  public static final Parcelable.Creator<ThemeColor> CREATOR =
      new Parcelable.Creator<>() {
        @Override
        public ThemeColor createFromParcel(Parcel source) {
          return new ThemeColor(source);
        }

        @Override
        public ThemeColor[] newArray(int size) {
          return new ThemeColor[size];
        }
      };

  private final int original;
  private final int colorOnPrimary;
  private final int colorPrimary;
  private final boolean isDark;

  public ThemeColor(Context context, int color) {
    this(context, color, color);
  }

  public ThemeColor(Context context, int original, int color) {
    this.original = original;
    if (color == 0) {
      color = TasksThemeKt.BLUE;
    } else {
      color |= 0xFF000000; // remove alpha
    }
    colorPrimary = color;

    double contrast = calculateContrast(WHITE, colorPrimary);
    isDark = contrast < 3;
    if (isDark) {
      colorOnPrimary = context.getColor(R.color.black_87);
    } else {
      colorOnPrimary = WHITE;
    }
  }

  private ThemeColor(Parcel source) {
    colorOnPrimary = source.readInt();
    colorPrimary = source.readInt();
    isDark = ParcelCompat.readBoolean(source);
    original = source.readInt();
  }

  public static ThemeColor getLauncherColor(Context context, int index) {
    return new ThemeColor(context, context.getColor(LAUNCHER_COLORS[index]));
  }

  public void applyToNavigationBar(Activity activity) {
    activity.getWindow().setNavigationBarColor(getPrimaryColor());

    View decorView = activity.getWindow().getDecorView();
    int systemUiVisibility = applyLightNavigationBar(decorView.getSystemUiVisibility());
    decorView.setSystemUiVisibility(systemUiVisibility);
  }

  private int applyLightNavigationBar(int flag) {
    return isDark
        ? flag | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        : flag & ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
  }

  @Override
  public int getPickerColor() {
    return colorPrimary;
  }

  @Override
  public boolean isFree() {
    switch (original) {
      case -14575885: // blue_500
      case -10453621: // blue_grey_500
      case -14606047: // grey_900
        return true;
      default:
        return false;
    }
  }

  public int getOriginalColor() {
    return original;
  }

  @ColorInt
  public int getPrimaryColor() {
    return colorPrimary;
  }

  @ColorInt
  public int getColorOnPrimary() {
    return colorOnPrimary;
  }

  public boolean isDark() {
    return isDark;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(colorOnPrimary);
    dest.writeInt(colorPrimary);
    ParcelCompat.writeBoolean(dest, isDark);
    dest.writeInt(original);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ThemeColor)) {
      return false;
    }

    ThemeColor that = (ThemeColor) o;

    return original == that.original;
  }

  @Override
  public int hashCode() {
    return original;
  }
}
