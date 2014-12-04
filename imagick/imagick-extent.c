#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_IN_FILE "/var/www/html/t.png"
#define PNG_OUT_FILE "/var/www/html/test.png"

/* gcc -o imagick imagick-extent.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main()
{
  MagickWand *mw;
  PixelWand  *pw;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();
  assert(mw);

  pw = NewPixelWand();
  assert(pw);

  PixelSetColor(pw, "blue");
  
  mbt = MagickReadImage(mw, PNG_IN_FILE);
  assert(mbt == MagickTrue);

  size_t w = MagickGetImageWidth(mw);
  size_t h = MagickGetImageHeight(mw);

  mbt = MagickSetImageBackgroundColor(mw, pw);
  assert(mbt == MagickTrue);

  // Note that the extent's offset is relative to the
  // top left corner of the *original* image, so adding an extent
  // around it means that the offset will be negative
  mbt = MagickExtentImage(mw, w*2, h*2, -(w/2), -(h/2));
  assert(mbt == MagickTrue);

  do {
    int a = -w/2;
    int b = -(w/2);
    printf("%d, %d\n", a, b);
  } while (0);

  do {
    ssize_t a = -w/2;
    ssize_t b = -(w/2);
    printf("%ld, %ld\n", a, b);
  } while (0);

  mbt = MagickSetImageFormat(mw, "png");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(mw, PNG_OUT_FILE);
  assert(mbt == MagickTrue);

  mw = DestroyMagickWand(mw);
  pw = DestroyPixelWand(pw);
  
  MagickWandTerminus(); /* shutdown */

  return 0;
}  
