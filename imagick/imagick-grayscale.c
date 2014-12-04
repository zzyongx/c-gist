#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_OUT_FILE1 "/var/www/html/test1.png"
#define PNG_OUT_FILE2 "/var/www/html/test2.png"

/* gcc -o imagick imagick-grayscale.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main()
{
  MagickWand *mw;
  PixelWand  *pw;
  PixelWand **pwptr;
  PixelIterator *ite;
  MagickBooleanType mbt;
  size_t w, h;
  size_t x, y;
  int gray;
  char hex[8];

  MagickWandGenesis();  /* startup */

  w = 400;
  h = 150;

  pw = NewPixelWand();
  // PixelSetColor(pw, "black"); // useless

  mw = NewMagickWand();
  mbt = MagickNewImage(mw, w, h, pw);
  assert(mbt == MagickTrue);

  ite = NewPixelIterator(mw);
  for (y = 0; y < h; ++y) {
    pwptr = PixelGetNextIteratorRow(ite, &x);
    for (x = 0; x < w; ++x) {
      // gray = x * y * 255/(w * h);
      gray = x * 255 / w;
      sprintf(hex, "#%02x%02x%02x", gray, gray, gray);
      PixelSetColor(pwptr[x], hex);
    }
    PixelSyncIterator(ite);
  }

  mbt = MagickSetImageFormat(mw, "png");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(mw, PNG_OUT_FILE1);
  assert(mbt == MagickTrue);

  ClearPixelWand(pw);
  ClearPixelIterator(ite);
  ClearMagickWand(mw);

  w = 150;
  h = 400;

  mbt = MagickNewImage(mw, w, h, pw);
  assert(mbt == MagickTrue);

  ite = NewPixelIterator(mw);
  for (y = 0; y < h; ++y) {
    pwptr = PixelGetNextIteratorRow(ite, &x);
    for (x = 0; x < w; ++x) {
      gray = y * 255 / h;
      sprintf(hex, "#%02x%02x%02x", gray, gray, gray);
      PixelSetColor(pwptr[x], hex);
    }
    PixelSyncIterator(ite);
  }

  mbt = MagickSetImageFormat(mw, "png");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(mw, PNG_OUT_FILE2);
  assert(mbt == MagickTrue);  

  pw = DestroyPixelWand(pw);
  ite = DestroyPixelIterator(ite);
  mw = DestroyMagickWand(mw);

  MagickWandTerminus(); /* shutdown */

  return 0;
}  
