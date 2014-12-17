#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <wand/magick_wand.h>

/* gcc -o imagick imagick-resize.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand */
int main(int argc, char *argv[])
{
  if (argc < 3) {
    fprintf(stderr, "%s inf outf [options]\n", argv[0]);
    return EXIT_FAILURE;
  }

  int options = 0;
  if (argc == 4) options = atoi(argv[3]);
  
  MagickWand *mw;
  MagickBooleanType mbt;
  const char *mformat;

  MagickWandGenesis();  /* startup */

  mw = NewMagickWand();
  assert(mw);

  mbt = MagickReadImage(mw, argv[1]);
  assert(mbt == MagickTrue);

  size_t w = MagickGetImageWidth(mw);
  size_t h = MagickGetImageHeight(mw);

  if ((w /= 2) < 1) w = 1;
  if ((h /= 2) < 1) h = 1;

  mformat = MagickGetImageFormat(mw);
  if (strcmp(mformat, "GIF") == 0) {
    size_t i;
    MagickWand *aw, *tw;

    size_t delay = MagickGetImageDelay(mw);
    
    aw = MagickCoalesceImages(mw);
    mw = DestroyMagickWand(mw);

    if (options == 1 || options == 2) {
      if (options == 1) {
        MagickSetIteratorIndex(aw, 0);
      } else {
        MagickSetIteratorIndex(aw, MagickGetNumberImages(aw)-1);
      }
      mw = MagickGetImage(aw);
      MagickResizeImage(mw, w, h, LanczosFilter, 1);
    } else if (options == 3) {
      PixelWand *pw = NewPixelWand();
      PixelSetColor(pw, "white");
      
      size_t w = MagickGetImageWidth(aw) * MagickGetNumberImages(aw);
      size_t h = MagickGetImageHeight(aw);
      mw = NewMagickWand();
      MagickNewImage(mw, w, h, pw);
      
      for (i = 0; i < MagickGetNumberImages(aw); ++i) {
        MagickSetIteratorIndex(aw, i);
        tw = MagickGetImage(aw);
        MagickCompositeImage(mw, tw, OverCompositeOp, MagickGetImageWidth(aw) * i,0);
        DestroyMagickWand(tw);
      }
      DestroyPixelWand(pw);
    } else {
      mw = NewMagickWand();
      // MagickSetImageFormat(mw, "GIF");
      MagickSetImageDelay(mw, delay);
      for (i = 0; i < MagickGetNumberImages(aw); ++i) {
        MagickSetIteratorIndex(aw, i);
        tw = MagickGetImage(aw);
        mbt = MagickResizeImage(tw, w, h, LanczosFilter, 1);
        assert(mbt == MagickTrue);
        MagickAddImage(mw, tw);
        // MagickSetImageDelay(mw, delay);        
        DestroyMagickWand(tw);
      }
      MagickResetIterator(mw);
      // gif = MagickCompareImageLayers(mw, CompareAnyLayer);
      // gif = MagickDeconstructImages(mw);
      mbt = MagickWriteImages(mw, argv[2], MagickTrue);
      mw = DestroyMagickWand(mw);
    }
  } else {
    mbt = MagickResizeImage(mw, w, h, LanczosFilter, 1);
    assert(mbt == MagickTrue);
  }

  if (mw) {
    MagickSetImageCompressionQuality(mw, 95);    
    mbt = MagickWriteImage(mw, argv[2]);
    assert(mbt == MagickTrue);

    mw = DestroyMagickWand(mw);
    assert(mw == NULL);
  }
  
  MagickWandTerminus(); /* shutdown */
  return 0;
}
