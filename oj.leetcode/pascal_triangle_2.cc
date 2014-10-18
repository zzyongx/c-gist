#include <iostream>
#include <vector>

class Solution {
public:
  std::vector<int> getRow(int rowIndex) {
    std::vector<int> v(rowIndex+1, 0);
    for (int i = 0; i < rowIndex+1; ++i) {
      if (i == 0) {
        v[0] = 1;
      } else if (i == 1) {
        v[1] = 1;
      } else {
        int x = v[0];
        for (int j = 1; j < i/2+1; ++j) {
          int t = v[j];
          v[j] += x;
          x = t;
        }
        for (int j = i/2+1; j <= i; ++j) {
          v[j] = v[i-j];
        }
      }
    }
    return v;
  }
};

void printVector(const std::vector<int> &v)
{
  for (size_t i = 0; i < v.size(); ++i) {
    std::cout << v[i] << " ";
  }
  std::cout << "\n";
}

int main()
{
  Solution s;

  std::vector<int> v = s.getRow(4);
  if (v[0] == 1 && v[1] == 4 && v[2] == 6 &&
      v[3] == 4 && v[4] == 1) {
    std::cout << "pascal row 5 TRUE\n";
  } else {
    std::cout << "pascal row 5 FALSE\n";
  }

  printVector(s.getRow(0));
  printVector(s.getRow(1));
  printVector(s.getRow(2));
  printVector(s.getRow(3));
  printVector(s.getRow(4));
  printVector(s.getRow(5));
  printVector(s.getRow(6));

  return 0;
}
