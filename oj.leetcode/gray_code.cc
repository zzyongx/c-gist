#include <iostream>
#include <vector>

class Solution {
public:
  std::vector<int> grayCode(int n) {
    std::vector<int> v;
    int num = 0;
    int i = 0;
    
    do {
      std::cout << num << " " << i << " ";
      v.push_back(num);
      num += (1 << i);

      ++i;
      if (i == n) {
        num << 1;
        i = 0;
      }
      num &= ((1 << n) - 1);
    } while (num != 0);

    return v;
  }
};

int main()
{
  Solution s;
  std::vector<int> nums;

  nums = s.grayCode(2);
  if (nums[0] == 0 && nums[1] == 1 && nums[2] == 3 && nums[3] == 2) {
    std::cout << "gray code 2 TRUE\n";
  } else {
    std::cout << "gray code 2 FALSE\n";
  }
  
  return 0;
}
