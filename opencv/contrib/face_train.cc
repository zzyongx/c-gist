#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <stddef.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <string>
#include <vector>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/contrib/contrib.hpp>

using namespace cv;

static bool isMaterialDir(const char *root, const char *name, std::string *path)
{
  for (const char *p = name; *p; ++p) {
    if (*p < '0' || *p > '9') return false;
  }
  path->assign(root).append("/").append(name);

  struct stat st;
  if (stat(path->c_str(), &st) != 0) {
    return false;
  }
  return S_ISDIR(st.st_mode);
}

static bool readMaterial(const char *root, std::vector<Mat> *images,
                         std::vector<int> *labels)
{
  DIR *dh = opendir(root);
  if (!dh) return false;
  
  size_t len = offsetof(struct dirent, d_name) + 256;
  struct dirent *rent = (struct dirent *) malloc(len);

  struct dirent *ent;
  while ((ent = readdir(dh))) {
    std::string path;
    if (!isMaterialDir(root, ent->d_name, &path)) continue;

    DIR *d = opendir(path.c_str());
    if (!d) return false;
    // printf("%s\n", ent->d_name);

    int label = atoi(ent->d_name);
    
    struct dirent *result;
    while (readdir_r(d, rent, &result) == 0 && result) {
      if ((rent->d_name[0] == '.' && rent->d_name[1] == '\0') ||
          (rent->d_name[0] == '.' && rent->d_name[1] == '.' && rent->d_name[2] == '\0')) {
        continue;
      }
      // printf("  %s\n", rent->d_name);
      
      Mat image = imread(path + "/" + rent->d_name, CV_LOAD_IMAGE_GRAYSCALE);
      if (!image.data) return false;
        
      images->push_back(image);
      labels->push_back(label);
    }
    closedir(d);
  }

  free(rent);
  closedir(dh);
  return true;
}

int main(int argc, char *argv[])
{
  if (argc != 2) {
    fprintf(stderr, "usage: %s workdir\n", argv[0]);
    return EXIT_FAILURE;
  }

  std::vector<Mat> images;
  std::vector<int> labels;

  if (!readMaterial(argv[1], &images, &labels)) {
    fprintf(stderr, "read metarial error %s\n", argv[1]);
    return EXIT_FAILURE;
  }

  Ptr<FaceRecognizer> model = createEigenFaceRecognizer();
  model->train(images, labels);
  model->save(std::string(argv[1]).append("/eigen.yaml"));

  return EXIT_SUCCESS;
}
