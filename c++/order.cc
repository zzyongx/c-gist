#include <cstdio>
#include <cstring>
#include <vector>
#include <iostream>
#include <iterator>
#include <algorithm>

using namespace std;

class Order {
public:
  virtual vector<string> &perform(vector<string> &v) = 0;
};

class IntOrder : public Order {
public:
  vector<string> &perform(vector<string> &v) {
    sort(v.begin(), v.end(), [](const string &a, const string &b) -> bool {
        return stoi(a) < stoi(b);
      });
    return v;
  }
};

class StringOrder : public Order {
public:
  vector<string> &perform(vector<string> &v) {
    sort(v.begin(), v.end());
    return v;
  }
};

class OrderFactory {
public:
  static Order *getInstance() {
    const char *config = getenv("ORDER_CONFIG");
    if (config && strcmp(config, "int") == 0) {
      return new IntOrder;
    } else {
      return new StringOrder;
    }
  }
};

class Main {
public:
  Main(int argc, char *argv[]) : argc_(argc), argv_(argv) {
    order_ = OrderFactory::getInstance();
  }
  void run() {
    vector<string> v(argv_+1, argv_+argc_);
    order_->perform(v);
    copy(v.begin(), v.end(), ostream_iterator<string>(cout, " "));
  }
private:
  Order  *order_;
  int     argc_;
  char  **argv_;
};

int main(int argc, char *argv[])
{
  Main main(argc, argv);
  main.run();
  return 0;
}
