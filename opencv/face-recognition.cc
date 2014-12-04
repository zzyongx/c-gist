#include <cstdio>
#include <vector>
#include <string>
#include <iostream>
#include <iterator>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/highgui/highgui.hpp>

using namespace cv;
// -lopencv_core -lopencv_highgui -lopencv_objdetect   

const std::string haarCascadeFile = 
  "/usr/share/OpenCV24/haarcascades/haarcascade_frontalface_alt2.xml";

int main(int argc, char *argv[])
{
  CascadeClassifier faceCascade;    
  if (!faceCascade.load(haarCascadeFile)) {
    std::cout << "Status: 500 Internal Error\n";
    std::cout << "Content-Type: text/plain\n";
    std::cout << "Stderr: load haar error\n\n";
    return EXIT_SUCCESS;
  }
  
  std::vector<uchar> data;
  std::cin >> std::noskipws;
  std::copy(std::istream_iterator<char>(std::cin),
            std::istream_iterator<char>(), std::back_inserter(data));

  Mat image;
  std::vector<Rect> faces;
  try {
    image = imdecode(Mat(data), 1);
    faceCascade.detectMultiScale(image, faces, 1.1, 3, CV_HAAR_SCALE_IMAGE,
                                 Size(30, 30), Size(0, 0));    
  } catch (const Exception &e) {
    std::cout << "Status: 400 Bad Request\n";
    std::cout << "Content-Type: text/plain\n";
    std::cout << "Stderr:  unsupported image type\n\n";
    return EXIT_SUCCESS;
  }

  if (faces.empty()) {
    std::cout << "Status: 404 Not Found\n";
    std::cout << "Content-Type: text/plain\n";
    std::cout << "Stderr: face not found\n\n";
    return EXIT_SUCCESS;
  }

  std::cout << "Content-Type: image/png\n";
  
  for (size_t i = 0; i < faces.size(); ++i) {
    std::cout << "Face" << i << "-X: " << faces[i].x << "\n";
    std::cout << "Face" << i << "-Y: " << faces[i].y << "\n";
    std::cout << "Face" << i << "-W: " << faces[i].width << "\n";
    std::cout << "Face" << i << "-H: " << faces[i].height << "\n";
    rectangle(image, faces[i], Scalar(255));
  }

  std::cout << "\n";

  imencode(".png", image, data);
  std::copy(data.begin(), data.end(),
            std::ostream_iterator<uchar>(std::cout));
    
  return EXIT_SUCCESS;
}
