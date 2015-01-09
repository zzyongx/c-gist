#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
#include <wand/magick_wand.h>

#define PNG_IN_FILE "/var/www/html/t.jpg"
#define PNG_OUT_FILE "/var/www/html/test.png"

/* gcc -o imagick imagick-coord-image.c -Wall -Werror -I/usr/include/ImageMagick -lMagickWand -std=c99 */
int main(int argc, char *argv[])
{
  if (argc < 3) {
    fprintf(stderr, "usage: %s infile outfile [width height]\n", argv[0]);
    return EXIT_FAILURE;
  }

  size_t width  = 1024;
  size_t height = 768;
  if (argc >= 4) width  = atoi(argv[3]);
  if (argc >= 5) height = atoi(argv[4]);
    
  MagickWand *mw, *mw2;
  PixelWand  *pw;
  DrawingWand *line;
  MagickBooleanType mbt;

  MagickWandGenesis();  /* startup */

  pw = NewPixelWand();
  PixelSetColor(pw, "transparent");

  mw = NewMagickWand();
  MagickNewImage(mw, width, height, pw);

  ClearPixelWand(pw);
  PixelSetColor(pw, "#c0c0c0");
  
  line = NewDrawingWand();
  DrawSetFillColor(line, pw);
  
  DrawLine(line, 0, 0, width-1, 0);
  DrawLine(line, width-1, 0, width-1, height-1);
  DrawLine(line, 0, height-1, width-1, height-1);
  DrawLine(line, 0, 0, 0, height-1);
  for (size_t i = 1; i < width/100+1; i++) {
    DrawLine(line, i * 100, 0, i * 100, height);
  }
  for (size_t i = 1; i < height/100+1; i++) {
    DrawLine(line, 0, i * 100, width, i * 100);
  }
  for (size_t i = 1; i < width/100+1; ++i) {
    for (size_t j = 1; j < height/100+1; ++j) {
      char buf[64];
      sprintf(buf, "(%zu, %zu)", i * 100, j * 100);
      DrawAnnotation(line, i * 100, j * 100, (unsigned char *) buf);
    }
  }

  mw2 = NewMagickWand();
  mbt = MagickReadImage(mw2, argv[1]);
  assert(mbt == MagickTrue);
  int w = (width - MagickGetImageWidth(mw2))/2;
  int h = (height - MagickGetImageHeight(mw2))/2;
  MagickCompositeImage(mw, mw2, OverCompositeOp, w > 0 ? w : 0, h > 0 ? h : 0);
  MagickDrawImage(mw, line);


  DestroyPixelWand(pw);
  DestroyDrawingWand(line);
  DestroyMagickWand(mw2);

  MagickSetImageFormat(mw, "png");
  mbt = MagickWriteImage(mw, argv[2]);
  assert(mbt == MagickTrue);

  DestroyMagickWand(mw);
  MagickWandTerminus(); /* shutdown */

  return EXIT_SUCCESS;
}  
