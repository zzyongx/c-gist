#include <iostream>
#include <vector>
#include <string>

class Solution {
public:
  std::vector<std::string> generateParenthesis_1(int n) {
    std::vector<std::string> v;
    v.push_back("()");
    
    for (int i = 1; i < n; ++i) {
      std::vector<std::string> last;
      last.swap(v);
      for (size_t j = 0; j < last.size(); ++j) {
        v.push_back("(" + last[j] + ")");
      }
      for (size_t j = 0; j < last.size()-1; ++j) {
        v.push_back(last[j] + "()");
      }
      for (size_t j = 0; j < last.size(); ++j) {
        v.push_back("()" + last[j]);
      }
    }
    return v;
  }
  
  void generateParenthesis(
    std::vector<std::string> *v, const std::string &t,
    int left, int right) {
    
    if (left == 0 && right == 0) {
      v->push_back(t);
    } else {
      if (left > 0) {
        generateParenthesis(v, t+'(', left-1, right);
      }
      if (left < right) {
        generateParenthesis(v, t+')', left, right-1);
      }
    }
  }
  
  std::vector<std::string> generateParenthesis(int n) {
    std::vector<std::string> v;
    if (n > 0) generateParenthesis(&v, "", n, n);
    return v;
  }
};

    
int main()
{
  Solution s;
  std::vector<std::string> r;
  
  r = s.generateParenthesis(3);
  for (size_t i = 0; i < r.size(); ++i) {
    std::cout << r[i] << " ";
  }
  std::cout << "\n";

  r = s.generateParenthesis(4);
  for (size_t i = 0; i < r.size(); ++i) {
    std::cout << r[i] << " ";
  }
  std::cout << "\n";
  
  return 0;
}
