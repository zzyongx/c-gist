#include <iostream>

class Solution {
public:
  int reverse(int x) {
    int n = 0;
    int sign = x > 0 ? 1 : -1;
    if (x < 0) x = -x;
    
    while (x) {
      if (x % 10 || n) {
        n = 10 * n + x % 10;
      }
      x /= 10;
    }
    return sign * n;
  }
};

int main()
{
  Solution s;
  int n;

  n = s.reverse(123);
  std::cout << "123 reverse " << (n == 321 ? "TRUE" : "FALSE") << "\n";

  n = s.reverse(-123);
  std::cout << "-123 reverse " << (n == -321 ? "TRUE" : "FALSE") << "\n";

  n = s.reverse(-12300);
  std::cout << "-12300 reverse " << (n == -321 ? "TRUE" : "FALSE") << "\n";

  n = s.reverse(901000);
  std::cout << "901000 reverse " << (n == 109 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
