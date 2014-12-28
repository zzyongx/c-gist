#include <cstdlib>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#define JPG  "/var/www/html/t.png"
#define ERODE_WIND  "erosion"
#define DILATE_WIND "dilation"

Mat src, erode_dst, dilate_dst;

int erosion_elem = 0;
int erosion_size = 2;
int dilation_elem = 0;
int dilation_size = 2;
const int max_elem = 2;
const int max_kernel_size = 21;

void erosion(int, void *)
{
  int type;
  if (erosion_elem == 0) type = MORPH_RECT;
  else if (erosion_elem == 1) type = MORPH_CROSS;
  else if (erosion_elem == 2) type = MORPH_ELLIPSE;

  Mat mat = getStructuringElement(type,
				  Size(2 * erosion_size + 1, 2 * erosion_size + 1),
				  Point(erosion_size, erosion_size));
  erode(src, erode_dst, mat);
  imshow(ERODE_WIND, erode_dst);
}

void dilation(int, void *)
{
  int type;
  if (dilation_elem == 0) type = MORPH_RECT;
  else if (dilation_elem == 1) type = MORPH_CROSS;
  else if (dilation_elem == 2) type = MORPH_ELLIPSE;

  Mat mat = getStructuringElement(type,
				  Size(2 * dilation_size + 1, 2 * dilation_size + 1),
				  Point(dilation_size, dilation_size));
  dilate(src, dilate_dst, mat);
  imshow(DILATE_WIND, dilate_dst);
}

int main(int argc, char *argv[])
{
  src = imread(JPG);

  namedWindow(ERODE_WIND, CV_WINDOW_AUTOSIZE);
  namedWindow(DILATE_WIND, CV_WINDOW_AUTOSIZE);
  moveWindow(DILATE_WIND, src.cols, 0);

  createTrackbar("Element:\n 0:Rect \n 1:Cross \n 2:Ellipse",
		 ERODE_WIND, &erosion_elem, max_elem, erosion);
  createTrackbar("Kernel Size:\n 2n + 1",
		 ERODE_WIND, &erosion_size, max_kernel_size, erosion);

  createTrackbar("Element:\n 0:Rect \n 1:Cross \n 2:Ellipse",
		 DILATE_WIND, &dilation_elem, max_elem, dilation);
  createTrackbar("Kernel Size:\n 2n + 1",
		 DILATE_WIND, &dilation_size, max_kernel_size, dilation);

  erosion(0, 0);
  dilation(0, 0);  

  waitKey(0);
  
  destroyWindow(ERODE_WIND);
  destroyWindow(DILATE_WIND);

  return EXIT_SUCCESS;
}  
