#include <string>
#include <vector>
#include <iostream>

class Solution {
public:
  int stoi(const char *ptr) {
    int i = 0;
    if (*ptr == '-') return -1 * stoi(ptr+1);
    else if (*ptr == '+') return stoi(ptr+1);
    else {
      for (/**/; *ptr; ++ptr) {
        i = i * 10 + *ptr - '0';
      }
      return i;
    }
  }
      
  int evalRPN(std::vector<std::string> &tokens) {
    int i;

    std::string top = tokens.back();
    tokens.pop_back();

    int op = (top.size() == 1) ? top[0] : '#';
    
    switch (op) {
    case '+': return evalRPN(tokens) + evalRPN(tokens);
    case '-': i = evalRPN(tokens); return evalRPN(tokens) - i ;
    case '*': return evalRPN(tokens) * evalRPN(tokens);
    case '/': i = evalRPN(tokens); return evalRPN(tokens) / i;
    default: return stoi(top.c_str());
    }
  }
};

int main()
{
  Solution s;
  
  const char *e1[] = {"2", "1", "+", "3", "*"};
  std::vector<std::string> tok1(e1, e1 + 5);
  std::cout << "{2, 1, +, 3, *} = 9 evaluate " << (s.evalRPN(tok1) == 9 ? "TRUE" : "FALSE") << "\n";
  
  const char *e2[] = {"4", "13", "5", "/", "+"};
  std::vector<std::string> tok2(e2, e2 + 5);
  std::cout << "{4, 13, 5, /, +} = 6 evaluate " << (s.evalRPN(tok2) == 6 ? "TRUE" : "FALSE") << "\n";

  const char *e3[] = {"3","-4","+"};
  std::vector<std::string> tok3(e3, e3 + 3);
  std::cout << "{3, -4, +} = -1 evaluate " << (s.evalRPN(tok3) == -1 ? "TRUE" : "FALSE") << "\n";

  return 0;
}
