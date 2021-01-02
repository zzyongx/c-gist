/**
Given an array of words and a width maxWidth, format the text such that each line has exactly maxWidth characters and is fully (left and right) justified.

You should pack your words in a greedy approach; that is, pack as many words as you can in each line. Pad extra spaces ' ' when necessary so that each line has exactly maxWidth characters.

Extra spaces between words should be distributed as evenly as possible. If the number of spaces on a line do not divide evenly between words, the empty slots on the left will be assigned more spaces than the slots on the right.

For the last line of text, it should be left justified and no extra space is inserted between words.

Note:

    A word is defined as a character sequence consisting of non-space characters only.
    Each word's length is guaranteed to be greater than 0 and not exceed maxWidth.
    The input array words contains at least one word.


Example 1:

Input: words = ["This", "is", "an", "example", "of", "text", "justification."], maxWidth = 16
Output:
[
   "This    is    an",
   "example  of text",
   "justification.  "
]

Example 2:

Input: words = ["What","must","be","acknowledgment","shall","be"], maxWidth = 16
Output:
[
  "What   must   be",
  "acknowledgment  ",
  "shall be        "
]
Explanation: Note that the last line is "shall be    " instead of "shall     be", because the last line must be left-justified instead of fully-justified.
Note that the second line is also left-justified becase it contains only one word.

Example 3:

Input: words = ["Science","is","what","we","understand","well","enough","to","explain","to","a","computer.","Art","is","everything","else","we","do"], maxWidth = 20
Output:
[
  "Science  is  what we",
  "understand      well",
  "enough to explain to",
  "a  computer.  Art is",
  "everything  else  we",
  "do                  "
]


Constraints:
    1 <= words.length <= 300
    1 <= words[i].length <= 20
    words[i] consists of only English letters and symbols.
    1 <= maxWidth <= 100
    words[i].length <= maxWidth
**/

#include "test.h"
#include <cassert>
#include <vector>
#include <string>

using namespace std;

class Solution {
public:
  string fullJustifyJoin(const vector<string> &words, int start, int end, int width, int maxWidth) {
    assert(start < end);

    auto wordNum = end - start;
    if (wordNum == 1) {
      return words[start] + string(maxWidth-width, ' ');
    }

    string line;
    if (words.size() == size_t(end)) { // last line
      for (auto i = start; i < end; ++i) {
        if (line.empty()) {
            line = words[i];
        } else {
          line += " " + words[i];
        }
      }
      if (line.size() < size_t(maxWidth)) {
        line += string(size_t(maxWidth) - line.size(), ' ');
      }
      return line;
    }

    int spaceNum = maxWidth - width;
    int paddingWidth = spaceNum / (wordNum-1) + 1;
    int leftNumber = spaceNum % (wordNum-1);

    // std::cout << spaceNum << " " << paddingWidth << " " << leftNumber << "\n";

    for (auto i = start, j = 0; i < end; ++i, ++j) {
      if (line.empty()) {
        line = words[i];
      } else if (j <= leftNumber) {
        line += string(paddingWidth, ' ') + words[i];
      } else {
        line += string(paddingWidth-1, ' ') + words[i];
      }
    }
    return line;
  }

  vector<string> fullJustify(vector<string>& words, int maxWidth) {
    vector<string> lines;

    int width = 0;
    size_t start = 0;

    for (size_t i = 0; i < words.size(); ++i) {
      int w = width + words[i].size() + (i-start);
      if (w > maxWidth) {
        lines.push_back(fullJustifyJoin(words, start, i, width, maxWidth));
        start = i;
        width = words[i].size();
      } else {
        width += words[i].size();
      }
    }

    if (start < words.size()) {
      lines.push_back(fullJustifyJoin(words, start, words.size(), width, maxWidth));
    }
    return lines;
  }
};

int main() {
  Solution solution;

  {
    vector<string> words = {"This", "is", "an", "example", "of", "text", "justification."};
    auto lines = solution.fullJustify(words, 16);

    vector<string> wants = {
      "This    is    an",
      "example  of text",
      "justification.  ",
    };

    if (wants != lines) {
      fatal("case1", wants, lines, "$$\n");
    }
  }

  {
    vector<string> words = {"What","must","be","acknowledgment","shall","be"};
    auto lines = solution.fullJustify(words, 16);

    vector<string> wants = {
      "What   must   be",
      "acknowledgment  ",
      "shall be        ",
    };

    if (wants != lines) {
      fatal("case2", wants, lines, "$$\n");
    }
  }

  {
    vector<string> words = {"Science","is","what","we","understand","well","enough","to","explain",
                            "to","a","computer.","Art","is","everything","else","we","do"};
    auto lines = solution.fullJustify(words, 20);

    vector<string> wants = {
      "Science  is  what we",
      "understand      well",
      "enough to explain to",
      "a  computer.  Art is",
      "everything  else  we",
      "do                  ",
    };

    if (wants != lines) {
      fatal("case3", wants, lines, "$$\n");
    }
  }
}
