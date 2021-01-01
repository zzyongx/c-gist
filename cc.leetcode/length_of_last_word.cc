#include <iostream>

class Solution {
public:
  int lengthOfLastWord(const char *s) {
    int len = 0;
    const char *token, *ptr;
    for (token = ptr = s; *ptr; ++ptr) {
      if (*ptr == ' ') {
        if (ptr != token) len = ptr - token;
        token = ptr+1;
      }
    }
    return ptr != token ? (ptr - token) : len;
  }
};

int main()
{
  Solution s;
  int n;

  n = s.lengthOfLastWord("");
  std::cout << "'' length of last word 0 " << (n == 0 ? "TRUE" : "FALSE") << "\n";

  n = s.lengthOfLastWord("Hello");
  std::cout << "'Hello' length of last word 5 " << (n == 5 ? "TRUE" : "FALSE") << "\n";

  n = s.lengthOfLastWord("Hello  World");
  std::cout << "'Hello  World' length of last word 5 " << (n == 5 ? "TRUE" : "FALSE") << "\n";

  n = s.lengthOfLastWord("Hello World  ");
  std::cout << "'Hello World  ' length of last word 5 " << (n == 5 ? "TRUE" : "FALSE") << "\n";  

  return 0;  
}
