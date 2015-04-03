#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

/* gcc -o imagick imagick-png-to-jpg-if-transparent.c -Wall -Werror `MagickWand-config --cflags --libs` */
int main(int argc, char *argv[])
{
  if (argc != 3) {
    fprintf(stderr, "%s inf outf\n", argv[0]);
    return EXIT_FAILURE;
  }
  
  MagickWand *mw;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();
  mbt = MagickReadImage(mw, argv[1]);
  assert(mbt == MagickTrue);

  mbt = MagickGetImageAlphaChannel(mw);
#if 0  
  if (mbt == MagickTrue) {
    PixelWand *pw = NewPixelWand();
    PixelSetColor(pw, "white");
    MagickSetImageBackgroundColor(mw, pw);    
    
    MagickWand *tmp = MagickMergeImageLayers(mw, FlattenLayer);
    pw = DestroyPixelWand(pw);
    DestroyMagickWand(mw);
    mw = tmp;
  }
#endif
  if (mbt == MagickTrue) {
    PixelWand *pw = NewPixelWand();
    PixelSetColor(pw, "white");

    printf("%s, %s\n", PixelGetColorAsString(pw), PixelGetColorAsNormalizedString(pw));
    printf("%f %f %f\n", PixelGetRed(pw), PixelGetGreen(pw), PixelGetBlue(pw));

    MagickWand *tmp = NewMagickWand();
    MagickNewImage(tmp, MagickGetImageWidth(mw), MagickGetImageHeight(mw), pw);
    MagickCompositeImage(tmp, mw, OverCompositeOp, 0, 0);

    pw = DestroyPixelWand(pw);
    DestroyMagickWand(mw);
    mw = tmp;
  }
  
  mbt = MagickWriteImage(mw, argv[2]);
  assert(mbt == MagickTrue);

  mw = DestroyMagickWand(mw);
  MagickWandTerminus(); /* shutdown */

  return 0;
}  
