#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_IN_FILE "/var/www/html/t.png"
#define PNG_OUT_FILE "/var/www/html/test.png"

/* gcc -o imagick imagick-resize.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main()
{
  MagickWand *mw;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();
  assert(mw);

  mbt = MagickReadImage(mw, PNG_IN_FILE);
  assert(mbt == MagickTrue);

  size_t w = MagickGetImageWidth(mw);
  size_t h = MagickGetImageHeight(mw);

  if ((w /= 2) < 1) w = 1;
  if ((h /= 2) < 1) h = 1;

  mbt = MagickResizeImage(mw, w, h, LanczosFilter, 1);
  assert(mbt == MagickTrue);
  
  mbt = MagickWriteImage(mw, PNG_OUT_FILE);
  assert(mbt == MagickTrue);

  MagickSetImageCompressionQuality(mw, 95);

  mw = DestroyMagickWand(mw);
  assert(mw == NULL);
  
  MagickWandTerminus(); /* shutdown */

  return 0;
}
