#include <iostream>
#include <vector>

class Solution {
public:
  std::vector< std::vector<int> > generate(int numRows) {
    std::vector<int> lastv;
    std::vector< std::vector<int> > vv;
    
    for (int i = 0; i < numRows; ++i) {
      std::vector<int> v;
      if (i == 0) {
        v.push_back(1);
      } else {
        v.push_back(1);
        for (size_t j = 0; j < lastv.size()-1; j++) {
          v.push_back(lastv[j] + lastv[j+1]);
        }
        v.push_back(1);
      }
      vv.push_back(v);
      lastv.swap(v);
    }
    return vv;
  }
};

int main()
{
  Solution s;
  std::vector< std::vector<int> > vv;

  vv = s.generate(5);
  if (vv[0][0] == 1 &&
      vv[1][0] == 1 && vv[1][1] == 1 &&
      vv[2][0] == 1 && vv[2][1] == 2 && vv[2][2] == 1 &&
      vv[3][0] == 1 && vv[3][1] == 3 && vv[3][2] == 3 && vv[3][3] == 1 &&
      vv[4][0] == 1 && vv[4][1] == 4 && vv[4][2] == 6 && vv[4][3] == 4 && vv[4][4] == 1) {
    std::cout << "pascal triangle 5 TRUE\n";
  } else {
    std::cout << "pascal triangle 5 FALSE\n";
  }

  return 0;
}  
