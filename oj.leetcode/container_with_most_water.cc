#include <iostream>
#include <vector>

class Solution {
public:
  int maxArea_1(std::vector<int> &height) {
    int area = 0;
    for (size_t i = 0; i < height.size() -1; ++i) {
      for (size_t j = i+1; j < height.size(); j++) {
        int min = height[i] < height[j] ? height[i] : height[j];
        if (min * int(j-i) > area) area = min * (j-i);
      }
    }
    return area;
  }
  
  int maxArea(std::vector<int> &height) {
    size_t left = 0;
    size_t right = height.size()-1;
    int area = 0;
    while (left < right) {
      int t;
      if (height[left] < height[right]) {
        t = height[left] * (right-left);
        left++;
      } else {
        t = height[right] * (right-left);
        right--;
      }
      if (t > area) area = t;
    }
    return area;
  }
};

int main()
{
  Solution s;
  int A[] = {488,8584,8144,7414,6649,3463,3453,8665,8006,1313,3815,7404,6969,7759,3643,8530};
  std::vector<int> v(A, A+sizeof(A)/sizeof(int));
  if (s.maxArea(v) == s.maxArea_1(v)) {
    std::cout << "maxArea TRUE\n";
  } else {
    std::cout << "maxArea FALSE\n";
  }
  return 0;
}
