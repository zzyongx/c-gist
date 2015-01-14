#include <cstdio>
#include <cstdlib>
#include <string>
#include <vector>
#include <time.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

static const int maxWidth = 200;
static const int maxHeight = 200;
static const int maxPadding = 50;
static const char *fileExt = ".png";

const std::string haarCascadeFile =
  "/usr/share/OpenCV24/haarcascades/haarcascade_frontalface_alt2.xml";

static void addPadding(Rect *rect, Mat *image)
{
  int padding = maxPadding;
  if (rect->x >= padding) {
    rect->x -= padding;
    padding *= 2;
  }
  if (rect->x + rect->width + padding <= image->cols) rect->width += padding;

  padding = maxPadding;
  if (rect->y >= padding) {
    rect->y -= padding;
    padding *= 2;
  }
  if (rect->y + rect->height + padding < image->rows) rect->height += padding;
}
  
static bool getFaceRect(Mat image, CascadeClassifier *classifier, Rect *rect)
{
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
    
    classifier->detectMultiScale(image, faces, 1.1, 3, CV_HAAR_SCALE_IMAGE,
                                Size(60, 60));
  } catch (const Exception &e) {
    fprintf(stderr, "face detect error %s\n", e.what());
    return false;
  }

  if (faces.empty()) {
    fprintf(stderr, "no face found\n");
    return false;
  }

  rect->width = rect->height = 0;
  for (size_t i = 0; i < faces.size(); ++i) {
    if (faces[i].width > rect->width && faces[i].height > rect->height) {
      *rect = faces[i];
    }
  }

  // addPadding(rect, &image);
  return true;
}

static std::string unUsedName(const char *root, const char *id)
{
  std::string dir(root);
  dir.append(1, '/').append(id);

  if (access(dir.c_str(), F_OK) != 0) {
    if (mkdir(dir.c_str(), 0755) != 0) return false;
  }
  
  for (size_t i = 0; i < 100; ++i) {
    std::string path(dir);
    path.append("/").append(1, '0' + i / 10).append(1, '0' + i % 10).append(fileExt);

    struct stat st;
    if (stat(path.c_str(), &st) != 0) return path;
  }

  srand(time(NULL));
  size_t i = rand() % 100;
  std::string path(root);
  path.append("/").append(1, '0' + i / 10).append(1, '0' + i % 10).append(fileExt);
  return path;
}

int main(int argc, char *argv[])
{
  if (argc != 4) {
    fprintf(stderr, "usage: %s wdir image label\n", argv[0]);
    return EXIT_FAILURE;
  }

  CascadeClassifier classifier;
  if (!classifier.load(haarCascadeFile)) {
    fprintf(stderr, "load classifier file %s error\n", haarCascadeFile.c_str());
    return EXIT_FAILURE;
  }

  Mat image = imread(argv[2], CV_LOAD_IMAGE_COLOR);
  if (!image.data) {
    fprintf(stderr, "load image %s error\n", argv[2]);
    return EXIT_FAILURE;
  }

  Rect rect;
  if (!getFaceRect(image, &classifier, &rect)) {
    return EXIT_FAILURE;
  }

  Mat small;
  resize(image(rect), small, Size(maxWidth, maxHeight));

  std::string path = unUsedName(argv[1], argv[3]);
  imwrite(path, small);

  return EXIT_SUCCESS;  
}
