#include <stdio.h>
#include <string.h>
#include <gd.h>

#define IDIR "/tmp/images/gd-watermark"

int main()
{
  gdImagePtr im;
  int white, black;
  int brect[8];
  char *err;

  char buf[128] = "Gd-Image";
  FILE *fp = fopen("/tmp/utf8.txt", "r");
  if (fp) {
    fgets(buf, sizeof(buf)-1, fp);
    fclose(fp);
  }

  double sz = 12.;
  const char *f = "/usr/share/fonts/simfang.ttf";  /* User supplied font */

  im = gdImageCreate(400, 300);

  /* Background color (first allocated) */
  white = gdImageColorResolve(im, 255, 255, 255);
  black = gdImageColorResolve(im, 0, 0, 0);

  err = gdImageStringFT(im, brect, black, f, sz, 0.0, 10, 40, buf);
  if (err) {
    fprintf(stderr,err); return 1;
  }

  fp = fopen(IDIR ".0.png", "w");
  if (fp) {
    /* Write img to FILE */
    gdImagePng(im, fp);
    fclose(fp);
  }

  /* Destroy it */
  gdImageDestroy(im);

  return 0;
}
