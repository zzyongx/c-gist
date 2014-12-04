#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_IN_FILE "/var/www/html/wizard.png"
#define PNG_OUT_FILE "/var/www/html/test.png"
#define JPG_OUT_FILE "/var/www/html/test.jpg"

/* gcc -o imagick imagick-floodfill.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main()
{
  MagickWand *mw;
  PixelWand  *fillpw;
  PixelWand  *borderpw;
  ChannelType channel;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();
  assert(mw);

  fillpw = NewPixelWand();
  assert(fillpw);

  borderpw = NewPixelWand();
  assert(borderpw);

  PixelSetColor(fillpw, "none");
  PixelSetColor(borderpw, "white");

  channel = ParseChannelOption("rgba");

  mbt = MagickReadImage(mw, PNG_IN_FILE);
  assert(mbt);

  mbt = MagickFloodfillPaintImage(mw, channel, fillpw, 10, borderpw, 0, 0, MagickFalse);
  assert(mbt);

  mbt = MagickSetImageFormat(mw, "png");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(mw, PNG_OUT_FILE);
  assert(mbt == MagickTrue);

  mbt = MagickSetImageFormat(mw, "jpg");
  assert(mbt == MagickTrue);
  mbt = MagickWriteImage(mw, JPG_OUT_FILE);
  assert(mbt == MagickTrue);

  mw = DestroyMagickWand(mw);
  fillpw = DestroyPixelWand(fillpw);
  borderpw = DestroyPixelWand(borderpw);
  
  MagickWandTerminus(); /* shutdown */

  return 0;
}  
