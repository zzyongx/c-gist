#include <iostream>
#include <string>

class Solution {
public:
  const char **units() {
    static const char *tbs[] = {
      "IX", "VIII", "VII", "VI", "V",
      "IV", "III", "II", "I", NULL};
    return tbs;
  }
  
  const char **tens() {
    static const char *tbs[] = {
      "XC", "LXXX", "LXX", "LX", "L",
      "XL", "XXX", "XX", "X", NULL};
    return tbs;
  }

  const char **hundreds() {
    static const char *tbs[] = {
      "CM", "DCCC", "DCC", "DC", "D",
      "CD", "CCC", "CC", "C", NULL};
    return tbs;
  }

  const char **thousands() {
    static const char *tbs[] = {
      "MMM", "MM", "M", NULL};
    return tbs;
  }
      
  int romanToInt(std::string s) {
    const char **tbs[] = {
      thousands(), hundreds(),
      tens(), units()};

    int num = 0;
    int idx = 0;
    
    for (int i = 0; i < 4; i++) {
      num *= 10;
      for (int j = 0; tbs[i][j]; j++) {
        size_t k;
        for (k = 0; tbs[i][j][k] && idx + k < s.size(); k++) {
          if (tbs[i][j][k] != s[idx + k]) break;
        }

        if (!tbs[i][j][k]) {
          int n = (i == 0) ? (3-j) : (9-j);
          num += n;
          idx += k;
          break;
        }
      }
    }
    return num;
  }
};

int main()
{
  Solution s;
  int num;

  num = s.romanToInt("MMMCMXCIX");
  std::cout << "3999 == MMMCMXCIX " << (num == 3999 ? "TRUE" : "FALSE") << "\n";

  num = s.romanToInt("MCMLXXVI");
  std::cout << "1976 == MCMLXXVI " << (num == 1976 ? "TRUE" : "FALSE") << "\n";

  num = s.romanToInt("MCMLXXXIV");
  std::cout << "1984 == MCMLXXXIV " << (num == 1984 ? "TRUE" : "FALSE") << "\n";

  num = s.romanToInt("MCM");
  std::cout << "1900 == MCM " << (num == 1900 ? "TRUE" : "FALSE") << "\n";

  num = s.romanToInt("MDCCCXCIX");
  std::cout << "1899 == MDCCCXCIX " << (num == 1899 ? "TRUE" : "FALSE") << "\n";

  num = s.romanToInt("MDCCCLXXXVIII");
  std::cout << "1888 == MDCCCLXXXVIII " << (num == 1888 ? "TRUE" : "FALSE") <<"\n";

  num = s.romanToInt("MDCLXVI");
  std::cout << "1666 == MDCLXVI " << (num == 1666 ? "TRUE" : "FALSE") << "\n";
                                            
  return 0;
}
