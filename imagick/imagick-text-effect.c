#include <stdio.h>
#include <wand/magick_wand.h>

/* gcc -o imagick -Werror -Wall -g imagick-text-effect.c -I/usr/include/ImageMagick -lMagickWand */

#define IDIR "/tmp/images/text-effect"

void set_tile_pattern(DrawingWand *d_wand, const char *pattern_name, const char *pattern_file)
{
	MagickWand *t_wand;
	long w,h;

	t_wand=NewMagickWand();  
	MagickReadImage(t_wand,pattern_file);
	// Read the tile's width and height
	w = MagickGetImageWidth(t_wand);
	h = MagickGetImageHeight(t_wand);

	DrawPushPattern(d_wand, pattern_name+1, 0, 0, w, h);
	DrawComposite(d_wand, SrcOverCompositeOp, 0, 0, 0, 0, t_wand);
	DrawPopPattern(d_wand);
	DrawSetFillPatternURL(d_wand, pattern_name);
}

int main()
{
  MagickWandGenesis();

  MagickWand  *mw, *mw2;
  DrawingWand *dw;
  PixelWand   *pw;

  mw = NewMagickWand();
  dw = NewDrawingWand();  
  
  pw = NewPixelWand();
  PixelSetColor(pw, "none");
  MagickNewImage(mw, 400, 150, pw);  // transparent image

  // 72px white font
  PixelSetColor(pw, "white");
  DrawSetFillColor(dw, pw);
  DrawSetFont(dw, "Verdana-Bold-Italic");
  DrawSetFontSize(dw, 72);

  // black outline
  PixelSetColor(pw, "black");
  DrawSetStrokeColor(dw, pw);

  // draw the text
  DrawSetTextAntialias(dw, MagickTrue);
  DrawAnnotation(dw, 50, 75, (unsigned char *) "Magick");
  MagickDrawImage(mw, dw);

  MagickWriteImage(mw, IDIR ".0.png");

  // trim the image down to include only text
  MagickTrimImage(mw, 0);
  MagickWriteImage(mw, IDIR ".1.png");

  MagickResetImagePage(mw, "");
  mw2 = CloneMagickWand(mw);

  PixelSetColor(pw, "blue");
  MagickSetImageBackgroundColor(mw, pw);
  MagickShadowImage(mw, 70, 4, 8, 8);
  MagickWriteImage(mw, IDIR ".2.png");

  MagickCompositeImage(mw, mw2, OverCompositeOp, 8, 8);
  MagickWriteImage(mw, IDIR ".3.png");

  ClearMagickWand(mw2);
  PixelSetColor(pw, "rgb(125,215,255)");
  MagickNewImage(mw2, MagickGetImageWidth(mw), MagickGetImageHeight(mw), pw);
  MagickCompositeImage(mw2, mw, OverCompositeOp, 0, 0);
  MagickWriteImage(mw2, IDIR ".4.png");

  ClearMagickWand(mw);
  ClearDrawingWand(dw);
  ClearPixelWand(pw);

  set_tile_pattern(dw, "#check", "pattern:checkerboard");

  PixelSetColor(pw, "lightblue");
  MagickNewImage(mw, 400, 150, pw);

  DrawSetFont(dw, "Verdana-Bold-Italic");
  DrawSetFontSize(dw, 72);
  DrawAnnotation(dw, 28, 68, (unsigned char *) "Magick");

  MagickDrawImage(mw, dw);
  MagickWriteImage(mw, IDIR ".5.png");

  DestroyMagickWand(mw);
  DestroyDrawingWand(dw);
  DestroyPixelWand(pw);

  return 0;  
}
