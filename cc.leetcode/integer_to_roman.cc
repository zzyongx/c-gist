#include <iostream>
#include <string>

class Solution {
public:
  const char *units(int i) {
    const char *tbs[] = {
      "", "I", "II", "III", "IV",
      "V", "VI", "VII", "VIII", "IX"};
    return tbs[i];
  }
  
  const char *tens(int i) {
    const char *tbs[] = {
      "", "X", "XX", "XXX", "XL",
      "L", "LX", "LXX", "LXXX", "XC"};
    return tbs[i];
  }

  const char *hundreds(int i) {
    const char *tbs[] = {
      "", "C", "CC", "CCC", "CD",
      "D", "DC", "DCC", "DCCC", "CM"};
    return tbs[i];
  }

  const char *thousands(int i) {
    const char *tbs[] = {
      "", "M", "MM", "MMM"};
    return tbs[i];
  }
      
  std::string intToRoman(int num) {
    std::string s;
    int n;
    
    n = num / 1000;
    s.append(thousands(n));
    num %= 1000;

    n = num / 100;
    s.append(hundreds(n));
    num %= 100;

    n = num / 10;
    s.append(tens(n));
    num %= 10;

    s.append(units(num));
    return s;    
  }
};

int main()
{
  Solution s;
  std::string roman;

  roman = s.intToRoman(3999);
  std::cout << "3999 == MMMCMXCIX " << (roman == "MMMCMXCIX" ? "TRUE" : "FALSE") << "\n";

  roman = s.intToRoman(1976);
  std::cout << "1976 == MCMLXXVI " << (roman == "MCMLXXVI" ? "TRUE" : "FALSE") << "\n";

  roman = s.intToRoman(1984);
  std::cout << "1984 == MCMLXXXIV " << (roman == "MCMLXXXIV" ? "TRUE" : "FALSE") << "\n";

  roman = s.intToRoman(1900);
  std::cout << "1900 == MCM " << (roman == "MCM" ? "TRUE" : "FALSE") << "\n";

  roman = s.intToRoman(1899);
  std::cout << "1899 == MDCCCXCIX " << (roman == "MDCCCXCIX" ? "TRUE" : "FALSE") << "\n";

  roman = s.intToRoman(1888);
  std::cout << "1888 == MDCCCLXXXVIII " << (roman == "MDCCCLXXXVIII" ? "TRUE" : "FALSE") <<"\n";

  roman = s.intToRoman(1666);
  std::cout << "1666 == MDCLXVI " << (roman == "MDCLXVI" ? "TRUE" : "FALSE") << "\n";
                                            
  return 0;
}
