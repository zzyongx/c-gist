#include <iostream>
#include <algorithm>
#include <vector>

class Solution {
public:
  std::vector<int> plusOne(const std::vector<int> &digits) {
    int plus = 1;
    for (int i = digits.size()-1; i >= 0; i--) {
      plus = (digits[i] + plus >= 10 ? 1 : 0);
    }
    
    std::vector<int> nums(digits.size()+plus);
    plus = 1;
    for (int i = digits.size()-1, j = nums.size()-1; i >= 0; i--, j--) {
      if (digits[i] + plus >= 10) {
        nums[j] = digits[i] + plus - 10;
      } else {
        nums[j] = digits[i] + plus;
        plus = 0;        
      }
    }
    if (plus) nums[0] = 1;
    return nums;
  }
};

std::vector<int> intToVec(int i)
{
  std::vector<int> v;
  do {
    v.push_back(i % 10);
    i /= 10;
  } while (i > 0);

  std::reverse(v.begin(), v.end());
  return v;
}

int vecToInt(const std::vector<int> &v) {
  size_t num = 0;
  for (size_t i = 0; i < v.size(); ++i) {
    num = num * 10 + v[i];
  }
  return num;
}

int main()
{
  Solution s;
  int n;

  n = vecToInt(s.plusOne(intToVec(1199)));
  std::cout << "1199 plus one = 1200 " << n << (n == 1200 ? "TRUE" : "FALSE") << "\n";

  n = vecToInt(s.plusOne(intToVec(999)));
  std::cout << "999 plus one = 1000 " << (n == 1000 ? "TRUE" : "FALSE") << "\n";

  return 0;
}  
