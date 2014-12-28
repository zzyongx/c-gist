#include <cstdlib>
#include <opencv2/imgproc/imgproc.hpp>
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

  Mat mat = imread(img);
  Mat dst;
  Canny(mat, dst, 0, 100);
  
  namedWindow(WIND, WINDOW_AUTOSIZE);
  imshow(WIND, dst);
  waitKey(0);

  destroyWindow(WIND);
  return EXIT_SUCCESS;
}  
