#include <iostream>

class Solution {
public:
  /* f(1) = 1
   * f(2) = 2
   * f(n) = f(n-1) + f(n-2)
   */
  int climbStairs(int n) {
    if (n == 1) return 1;
    if (n == 2) return 2;
    
    int p  = 2;
    int pp = 1;
    int ret;
    for (int i = 3; i <= n; i++) {
      ret = p + pp;
      pp = p;
      p = ret;
    }
    return ret;
  }
};

int main()
{
  Solution s;
  int n;

  n = s.climbStairs(1);
  std::cout << "1 stairs ways 1 " << (n == 1 ? "TRUE" : "FALSE") << "\n";

  n = s.climbStairs(2);
  std::cout << "2 stairs ways 2 " << (n == 2 ? "TRUE" : "FALSE") << "\n";

  n = s.climbStairs(3);
  std::cout << "3 stairs ways 3 " << (n == 3 ? "TRUE" : "FALSE") << "\n";

  n = s.climbStairs(4);
  std::cout << "4 stairs ways 5 " << (n == 5 ? "TRUE" : "FALSE") << "\n";

  n = s.climbStairs(5);
  std::cout << "5 stairs ways 8 " << (n == 8 ? "TRUE" : "FALSE") << "\n";

  return 0;  
}

