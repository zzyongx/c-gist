#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_IN_FILE "/var/www/html/t.png"
#define PNG_OUT_FILE "/var/www/html/test.png"

/* gcc -o imagick imagick-landscape-3d.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main()
{
  MagickWand *mw, *canvas;
  PixelWand  *pw;
  DrawingWand *line;
  size_t w, h, offset;
  int x, y, r, g, b, grey, lh;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();

  mbt = MagickReadImage(mw, PNG_IN_FILE);
  assert(mbt == MagickTrue);
  w = MagickGetImageWidth(mw);
  h = MagickGetImageHeight(mw);

  pw = NewPixelWand();
  PixelSetColor(pw, "transparent");

  mbt = MagickShearImage(mw, pw, 45, 0);
  assert(mbt == MagickTrue);

  w = MagickGetImageWidth(mw);
  h = MagickGetImageHeight(mw);

  mbt = MagickScaleImage(mw, w, h/2);
  assert(mbt = MagickTrue);

  w = MagickGetImageWidth(mw);
  h = MagickGetImageHeight(mw);

  canvas = NewMagickWand();
  MagickGetImagePixelColor(mw, 0, 0, pw);
  MagickNewImage(canvas, w, h*2, pw);

  offset = h;
  for (x = 0; x < w; ++x) {
    line = NewDrawingWand();
    lh = 0;
    for (y = h-1; y >= 0; --y) {
      if (MagickGetImagePixelColor(mw, x, y, pw) == MagickFalse) continue;
      r = 255 * PixelGetRed(pw);
      g = 255 * PixelGetGreen(pw);
      b = 255 * PixelGetBlue(pw);

      grey = (r + g + b)/5;
      if (lh == 0 || lh < grey) {
        DrawSetFillColor(line, pw);
        DrawSetStrokeColor(line, pw);
        DrawLine(line, x, y + offset - lh, x, y - grey + offset);
        lh = grey;
      }
      lh--;
    }

    MagickDrawImage(canvas, line);
    DestroyDrawingWand(line);
  }

  MagickScaleImage(canvas, w - h, h * 2);
  
  mbt = MagickSetImageFormat(canvas, "png");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(canvas, PNG_OUT_FILE);
  assert(mbt == MagickTrue);

  pw = DestroyPixelWand(pw);
  mw = DestroyMagickWand(mw);
  canvas = DestroyMagickWand(canvas);
  
  MagickWandTerminus(); /* shutdown */

  return 0;
}  
