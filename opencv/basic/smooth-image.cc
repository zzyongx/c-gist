#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#define JPG  "/var/www/html/t.jpg"
#define WIND "example"
#define MAX_KERNEL_LENGTH 11

Mat src, dst;

inline void display_caption(const char *caption)
{
  dst = Mat::zeros(src.size(), src.type());
  putText(dst, caption, Point(src.cols/4, src.rows/2),
	  CV_FONT_HERSHEY_COMPLEX, 1, Scalar(255, 255, 255));
  imshow(WIND, dst);
  waitKey(0);
}

inline void display_dst()
{
  imshow(WIND, dst);
  waitKey(0);
}

int main(int argc, char *argv[])
{
  namedWindow(WIND);
  src = imread(JPG);

  display_caption("Original Image");

  dst = src.clone();
  display_dst();

  display_caption("Homogeneous Blur");
  for (int i = 1; i < MAX_KERNEL_LENGTH; i+=2) {
    blur(src, dst, Size(i, i));
    display_dst();
  }

  display_caption("Gaussian Blur");
  for (int i = 1; i < MAX_KERNEL_LENGTH; i+=2) {
    GaussianBlur(src, dst, Size(i, i), 0, 0);
    display_dst();
  }

  display_caption("Median Blur");
  for (int i = 1; i < MAX_KERNEL_LENGTH; i+=2) {
    medianBlur(src, dst, i);
    display_dst();
  }

  display_caption("Bilateral Blur");
  for (int i = 1; i < MAX_KERNEL_LENGTH; i+=2) {
    bilateralFilter(src, dst, i, i*2, i/2);
    display_dst();
  }  
  
  return EXIT_SUCCESS;
}  
