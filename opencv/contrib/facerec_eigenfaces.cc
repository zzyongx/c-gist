#include <cstdio>
#include <cstdlib>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <algorithm>
#include <opencv2/core/core.hpp>
#include <opencv2/contrib/contrib.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;
#define WIND "window"

static Mat norm_0_255(Mat src)
{
  Mat dst;
  switch (src.channels()) {
  case 1:
    normalize(src, dst, 0, 255, NORM_MINMAX, CV_8UC1);
    break;
  case 3:
    normalize(src, dst, 0, 255, NORM_MINMAX, CV_8UC3);
    break;
  default:
    src.copyTo(dst);
    break;
  }
  return dst;
}

static void read_csv(const char *f, std::vector<Mat> &images, std::vector<int> &labels,
		     std::vector<Mat> &testImages, std::vector<int> &testLabels)
{
  std::ifstream file(f, std::ifstream::in);
  if (!file) {
    fprintf(stderr, "no invalid csv file was given\n");
    exit(EXIT_FAILURE);
  }

  std::string line, path, label;
  while (std::getline(file, line)) {
    std::stringstream liness(line);
    getline(liness, path, ';');
    getline(liness, label);
    if (!path.empty() && !label.empty()) {
      int lab = atoi(label.c_str());
      if (std::find(testLabels.begin(), testLabels.end(), lab) == testLabels.end()) {
	testImages.push_back(imread(path, CV_LOAD_IMAGE_GRAYSCALE));
	testLabels.push_back(lab);
      } else {
	images.push_back(imread(path, CV_LOAD_IMAGE_GRAYSCALE));
	labels.push_back(lab);
      }
    }
  }
}

int main(int argc, char *argv[])
{
  if (argc < 2) {
    fprintf(stderr, "usage: %s csvfile\n", argv[0]);
    return EXIT_FAILURE;
  }

  std::vector<Mat> images;
  std::vector<int> labels;
  std::vector<Mat> testImages;
  std::vector<int> testLabels;

  read_csv(argv[1], images, labels, testImages, testLabels);
  if (images.empty() or testImages.empty()) {
    fprintf(stderr, "demo needs at least 2 images\n");
    return EXIT_FAILURE;
  }

  int width  = images[0].cols;
  int height = images[0].rows;

  Ptr<FaceRecognizer> models[] = {
    createEigenFaceRecognizer(),
    createFisherFaceRecognizer(),
    //    createLBPHFaceRecognizer(),
    0,
  };

  namedWindow(WIND, WINDOW_AUTOSIZE);

  for (size_t i = 0; models[i]; ++i) {
    Ptr<FaceRecognizer> model = models[i];
    
    model->train(images, labels);

    for (size_t i = 0; i < testImages.size(); ++i) {
      int predictLabel = model->predict(testImages[i]);
      printf("Predict class = %d, Actual class = %d %s\n", predictLabel, testLabels[i],
	     predictLabel == testLabels[i] ? "OK" : "ERROR");
    }

    Mat eigenValues  = model->getMat("eigenvalues");
    Mat eigenVectors = model->getMat("eigenvectors");
    Mat mean         = model->getMat("mean");

    imshow(WIND, norm_0_255(mean.reshape(1, height)));
    waitKey(0);

    int n = std::min(10, eigenVectors.cols);
    Mat result(height * (n % 5 == 0 ? n / 5 : n / 5 + 1), width * 5, CV_8UC3);

    for (int i = 0; i < std::min(10, eigenVectors.cols); i++) {
      printf("eigenvalue #%d = %0.5f\n", i, eigenValues.at<double>(i));
      Mat ev = eigenVectors.col(i).clone();
      Mat grayScale = norm_0_255(ev.reshape(1, height));
      Mat cgrayScale;
      applyColorMap(grayScale, cgrayScale, COLORMAP_JET);
      cgrayScale.copyTo(result(Rect((i%5) * width, (i/5) * height, width, height)));
    }
    imshow(WIND, result);
    waitKey(0);

    n = (std::min(300, eigenVectors.cols) - std::min(10, eigenVectors.cols))/15;
    result = Mat(height * (n % 5 == 0 ? n / 5 : n / 5 + 1), width * 5, CV_8UC1);

    for (int i = std::min(10, eigenVectors.cols); i < std::min(300, eigenVectors.cols); i += 15) {
      Mat evs = Mat(eigenVectors, Range::all(), Range(0, i));
      Mat projection = subspaceProject(evs, mean, images[0].reshape(1, 1));
      Mat reconstruction = subspaceReconstruct(evs, mean, projection);
      reconstruction = norm_0_255(reconstruction.reshape(1, height));
      reconstruction.copyTo(result(Rect((i/15%5) * width, (i/15/5) * height, width, height)));
    }
    imshow(WIND, result);
    waitKey(0);
  }
  
  return EXIT_SUCCESS;
}
