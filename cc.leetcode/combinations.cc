#include <iostream>
#include <vector>

class Solution {
public:
  std::vector<std::vector<int> > combine(int start, int end, int k) {
    std::vector< std::vector<int> > vv;    
    if (k == 1) {
      for (int i = start; i < end; ++i) {
        vv.push_back(std::vector<int>(1, i));
      }
    } else {
      for (int i = start; i < end; i++) {
        std::vector< std::vector<int> > t = combine(i+1, end, k-1);
        for (size_t j = 0; j < t.size(); ++j) {
          std::vector<int> v(1, i);
          v.insert(v.end(), t[j].begin(), t[j].end());
          vv.push_back(v);
        }
      }
    }
    return vv;
  }
  std::vector<std::vector<int> > combine(int n, int k) {
    return combine(1, n+1, k);
  }
};

int main()
{
  Solution s;
  std::vector< std::vector<int> > vv = s.combine(4, 2);

  for (size_t i = 0; i < vv.size(); ++i) {
    for (size_t j = 0; j < vv[i].size(); ++j) {
      std::cout << vv[i][j] << " ";
    }
    std::cout << "\n";
  }

  return 0;
}  
