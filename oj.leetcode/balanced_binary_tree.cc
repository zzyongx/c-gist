#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  int min(int a, int b) {
    return a > b ? b : a;
  }

  bool limitDepth(TreeNode *root, int limit) {
    if (!root) return false;
    if (limit == 0) return true;

    if (limitDepth(root->left, limit-1)) return true;
    return limitDepth(root->right, limit-1);
  }

  int minDepth(TreeNode *root) {
    if (!root) return 0;
    int left = minDepth(root->left);
    if (left == 0) return 1;
    return min(left, minDepth(root->right)) + 1;
  }
  
  bool isBalanced(TreeNode *root) {
    if (!root) return true;    
    int minDep = minDepth(root);
    return !limitDepth(root, minDep+2);
  }
};

TreeNode *tree1()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);
  TreeNode *n4 = new TreeNode(4);
  TreeNode *n5 = new TreeNode(5);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n1->right = n4;

  n4->left = n5;

  return n0;
}

TreeNode *tree2()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);
  TreeNode *n4 = new TreeNode(4);
  TreeNode *n5 = new TreeNode(5);
  TreeNode *n6 = new TreeNode(6);
  
  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n1->right = n4;

  n3->left = n5;

  n5->left = n6;

  return n0;
}

int main()
{
  Solution s;
  bool b;

  TreeNode *t1 = tree1();
  b = s.isBalanced(t1);
  std::cout << "1, 2, 3, 4 is balanced " << (b ? "TRUE" : "FALSE") << "\n";

  TreeNode *t2 = tree2();
  b = s.isBalanced(t2);
  std::cout << "1, 2, 3, 4, 5 is not balanced " << (!b ? "TRUE" : "FALSE") << "\n";

  return 0;
}
