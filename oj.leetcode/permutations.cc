#include <iostream>
#include <vector>

class Solution {
public:
  std::vector< std::vector<int> > permute_1(std::vector<int> &num) {
    std::vector< std::vector<int> > vv;
    if (num.empty()) return vv;

    vv.push_back(std::vector<int>(num[0], 1));
    
    for (size_t i = 1; i < num.size(); ++i) {
      std::vector< std::vector<int> > last;
      last.swap(vv);
      for (size_t j = 0; j < last.size(); ++j) {
        last[j].push_back(num[i]);
        vv.push_back(last[j]);
        for (size_t k = last[j].size()-1; k > 0; k--) {
          int t = last[j][k];
          last[j][k] = last[j][k-1];
          last[j][k-1] = t;
          vv.push_back(last[j]);
        }
      }
    }
    
    return vv;
  }

  std::vector< std::vector<int> > permute_2(std::vector<int> &num) {
    std::vector< std::vector<int> > vv;
    if (num.empty()) return vv;

    if (num.size() == 1) {
      vv.push_back(std::vector<int>(1, num[0]));
    } else {
      for (size_t i = 0; i < num.size(); i++) {
        std::vector<int> num2(num.begin(), num.begin()+i);
        num2.insert(num2.end(), num.begin()+i+1, num.end());
      
        std::vector< std::vector<int> > t = permute_2(num2);
        for (size_t j = 0; j < t.size(); j++) {
          std::vector<int> v(1, num[i]);
          v.insert(v.end(), t[j].begin(), t[j].end());
          vv.push_back(v);
        }
      }
    }
    return vv;
  }
};

int main()
{
  Solution s;

  int A[] = {1, 2, 3, 4, 5, 6};
  std::vector<int> v(A, A+6);
  std::vector< std::vector<int> > vv = s.permute_2(v);

  for (size_t i = 0; i < vv.size(); ++i) {
    for (size_t j = 0; j < vv[i].size(); ++j) {
      std::cout << vv[i][j] << " ";
    }
    std::cout << "\n";
  }
  
  return 0;
}
