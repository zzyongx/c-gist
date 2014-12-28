#include <cstdlib>
#include <cstdio>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

#define WIND "morphology"
#define JPG  "/var/www/html/t1.jpg"

Mat src, dst;

int morph_elem = 0;
int morph_size = 0;
int morph_operator = 0;
int const max_operator = 4;
int const max_elem = 2;
int const max_kernel_size = 21;

/** Function Headers */
void Morphology_Operations( int, void* );

/** @function main */
int main( int argc, char** argv )
{
  src = imread(JPG);

  namedWindow(WIND, CV_WINDOW_AUTOSIZE);

  createTrackbar("Operator:\n 0: Opening - 1: Closing \n 2: Gradient - 3: Top Hat \n 4: Black Hat",
		 WIND, &morph_operator, max_operator, Morphology_Operations);
  createTrackbar("Element:\n 0: Rect - 1: Cross - 2: Ellipse",
		 WIND, &morph_elem, max_elem, Morphology_Operations );
  createTrackbar("Kernel size:\n 2n +1",
		 WIND, &morph_size, max_kernel_size, Morphology_Operations );

  Morphology_Operations(0, 0);

  waitKey(0);
  return 0;
}

 /**
  * @function Morphology_Operations
  */
void Morphology_Operations( int, void* )
{
  // Since MORPH_X : 2,3,4,5 and 6
  int operation = morph_operator + 2;

  Mat element = getStructuringElement( morph_elem, Size( 2*morph_size + 1, 2*morph_size+1 ), Point( morph_size, morph_size ) );

  /// Apply the specified morphology operation
  morphologyEx( src, dst, operation, element );
  imshow(WIND, dst );
}
