#include <cstdlib>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#define JPG  "/var/www/html/t.jpg"
#define WIND "example"

int main(int argc, char *argv[])
{
  const char *img = argc >= 2 ? argv[1] : JPG;
  int flags[] = {
    CV_LOAD_IMAGE_ANYDEPTH, CV_LOAD_IMAGE_COLOR, CV_LOAD_IMAGE_GRAYSCALE
  };
  for (size_t i = 0; i < 3; ++i) {
    Mat mat = imread(img, flags[i]);
    namedWindow(WIND, WINDOW_AUTOSIZE);
    imshow(WIND, mat);
    waitKey(0);
    destroyWindow(WIND);
  }
  return EXIT_SUCCESS;
}  
