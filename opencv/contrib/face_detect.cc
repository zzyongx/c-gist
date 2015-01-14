#include <cstdio>
#include <cstdlib>
#include <string>
#include <vector>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

const std::string haarCascadeFile =
  "/usr/share/OpenCV24/haarcascades/haarcascade_frontalface_alt2.xml";

int main(int argc, char *argv[])
{
  if (argc != 2) {
    fprintf(stderr, "usage: %s infile\n", argv[0]);
    return EXIT_FAILURE;
  }

  CascadeClassifier classifier;
  if (!classifier.load(haarCascadeFile)) {
    fprintf(stderr, "load classifier file %s error\n", haarCascadeFile.c_str());
    return EXIT_FAILURE;
  }

  Mat image = imread(argv[1], CV_LOAD_IMAGE_COLOR);
  if (!image.data) {
    fprintf(stderr, "load image %s error\n", argv[1]);
    return EXIT_FAILURE;
  }

  int factor = 1;
  std::vector<Rect> faces;

  try {
    Size size = image.size();
    if (size.width > 800 or size.height > 800) {
      int max = size.width > size.height ? size.width : size.height;
      factor = max / 800;
      Mat small;
      resize(image, small, Size(size.width/factor, size.height/factor));
      image = small;
    }
    
    classifier.detectMultiScale(image, faces, 1.1, 3, CV_HAAR_SCALE_IMAGE,
                                Size(60, 60));
  } catch (const Exception &e) {
    fprintf(stderr, "face detect error %s\n", e.what());
    return EXIT_FAILURE;
  }

  return faces.empty() ? EXIT_FAILURE : EXIT_SUCCESS;
}
