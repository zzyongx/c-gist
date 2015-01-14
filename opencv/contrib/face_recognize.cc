#include <cstdio>
#include <cstdlib>
#include <sys/types.h>
#include <sys/stat.h>
#include <opencv2/contrib/contrib.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;

static const int maxWidth = 200;
static const int maxHeight = 200;
static const int maxPadding = 50;

const std::string haarCascadeFile =
  "/usr/share/OpenCV24/haarcascades/haarcascade_frontalface_alt2.xml";

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

  return true;
}

int main(int argc, char *argv[])
{
  if (argc != 3) {
    fprintf(stderr, "usage: %s wdir image\n", argv[0]);
    return EXIT_FAILURE;
  }

  CascadeClassifier classifier;
  if (!classifier.load(haarCascadeFile)) {
    fprintf(stderr, "load classifier file %s error\n", haarCascadeFile.c_str());
    return EXIT_FAILURE;
  }

  Mat image = imread(argv[2], CV_LOAD_IMAGE_COLOR);
  if (!image.data) {
    fprintf(stderr, "%s is not a image\n", argv[2]);
    return EXIT_FAILURE;
  }

  Rect roi;
  if (!getFaceRect(image, &classifier, &roi)) return false;

  std::string modelFile(argv[1]);
  modelFile.append("/eigen.yaml");

  if (access(modelFile.c_str(), F_OK) != 0) {
    fprintf(stderr, "model file %s not exists\n", modelFile.c_str());
    return EXIT_FAILURE;
  }

  Ptr<FaceRecognizer> model = createEigenFaceRecognizer();
  model->load(modelFile);

  Mat small;
  resize(image(roi), small, Size(maxWidth, maxHeight));

  Mat grey;
  cvtColor(small, grey, CV_BGR2GRAY);

  imwrite("/var/www/html/facerec/grey.png", grey);

  int label = -1;
  double confidence = 0.0;  // the little the better
  model->predict(grey, label, confidence);
  printf("%d:%0.0f", label, confidence);

  return EXIT_SUCCESS;    
}
