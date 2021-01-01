#include <iostream>
#include <string>

class Solution {
public:
  void tidyWords(std::string &s) {
    bool first = false;
    size_t j = 0;
    for (size_t i = 0; i < s.size(); i++) {
      if (s[i] != ' ') {
        first = true;
        s[j++] = s[i];
      } else if (first) {
        first = false;
        s[j++] = s[i];
      }
    }
    if (j > 0 && s[j-1] == ' ') s.resize(j-1);
    else s.resize(j);
  }
  
  void reverseString(std::string &s, size_t begin, size_t end) {
    for (size_t i = begin, j = end - 1; i < j; ++i, --j) {
      s[i] = s[i] ^ s[j];
      s[j] = s[i] ^ s[j];
      s[i] = s[i] ^ s[j];
    }
  }
  
  void reverseWords(std::string &s) {
    tidyWords(s);
    size_t wb = 0;
    for (size_t i = 0; i < s.size(); ++i) {
      if (s[i] == ' ') {
        if (wb != i) reverseString(s, wb, i);
        wb = i+1;
      }
    }
    if (wb != 0) {
      if (wb != s.size()) reverseString(s, wb, s.size());
      reverseString(s, 0, s.size());
    }
  }
};

int main()
{
  Solution s;
  std::string str;

  str = " Hello  World  ";
  s.tidyWords(str);
  std::cout << "' Hello  World  ' tidy " << (str == "Hello World" ? "TRUE" : "FALSE") << "\n";

  str = "  Hello World ";
  s.tidyWords(str);
  std::cout << "'  Hello World ' tidy " << (str == "Hello World" ? "TRUE" : "FALSE") << "\n";

  str = " ";
  s.tidyWords(str);
  std::cout << "' ' tidy " << (str == "" ? "TRUE" : "FALSE") << "\n";

  str = "Hello World";
  s.reverseWords(str);
  std::cout << "'Hello World' reverse " << (str == "World Hello" ? "TRUE" : "FALSE") << "\n";

  str = "Hello Bei Jing";
  s.reverseWords(str);
  std::cout << "'Hello Bei Jing' reverse " << (str == "Jing Bei Hello" ? "TRUE" : "FALSE") << "\n";

  str = "Hello";
  s.reverseWords(str);
  std::cout << "'Hello' reverse " << (str == "Hello" ? "TRUE" : "FALSE") << "\n";

  str = " ";

  s.reverseWords(str);
  std::cout << "' ' reverse " << (str == "" ? "TRUE" : "FALSE") << "\n";
  

  return 0;
}
