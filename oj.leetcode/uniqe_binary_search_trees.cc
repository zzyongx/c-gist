#include <iostream>

class Solution {
public:
  int numTrees(int n) {
    if (n == 0) return 1;
    if (n <= 2) return n;
    
    int num = 0;
    for (int i = 0; i < n; ++i) {
      /* optimalize repeat calculate
       * ?? if symmetrical
       */
      num += numTrees(i) * numTrees(n-1-i);
    }
    return num;
  }
};

int main()
{
  Solution s;
  int n;

  n = s.numTrees(1);
  std::cout << "num trees(1) " << (n == 1 ? "TRUE" : "FALSE") << "\n";

  n = s.numTrees(2);
  std::cout << "num trees(2)= "  << (n == 2 ? "TRUE" : "FALSE") << "\n";

  n = s.numTrees(3);
  std::cout << "num trees(3)= " << (n == 5 ? "TRUE" : "FALSE") << "\n";
                                
  n = s.numTrees(4);            
  std::cout << "num trees(4)= " << (n == 14 ? "TRUE" : "FALSE") << "\n";
                                
  n = s.numTrees(7);            
  std::cout << "num trees(7)= " << (n == 429 ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
