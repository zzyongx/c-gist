#include <cstdlib>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#define JPG  "/var/www/html/t.jpg"
#define WIND "example"

int main(int argc, char *argv[])
{
  Mat mat = imread(JPG, CV_LOAD_IMAGE_COLOR);
  Mat dst1, dst2;
  pyrDown(mat, dst1);
  pyrUp(dst1, dst2);
  
  namedWindow(WIND, WINDOW_AUTOSIZE);
  
  imshow(WIND, mat);
  waitKey(0);
  
  imshow(WIND, dst1);
  waitKey(0);

  imshow(WIND, dst2);
  waitKey(0);
  
  destroyWindow(WIND);
  return EXIT_SUCCESS;
}  
