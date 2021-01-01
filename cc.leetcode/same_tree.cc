#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  bool isSameTree(TreeNode *p, TreeNode *q) {
    if (p && q && p->val == q->val) {
      return isSameTree(p->left, q->left) && isSameTree(p->right, q->right);
    } else {
      return (!p && !q);
    }      
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
  n2->right = n4;

  n3->right = n5;

  return n0;
}

TreeNode *tree2()
{
  TreeNode *n0 = new TreeNode(0);
  TreeNode *n1 = new TreeNode(1);
  TreeNode *n2 = new TreeNode(2);
  TreeNode *n3 = new TreeNode(3);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;

  return n0;
}

int main()
{
  Solution s;
  bool b;

  TreeNode *root1 = tree1();
  b = s.isSameTree(root1, root1);
  std::cout << "is same tree " << (b ? "TRUE" : "FALSE") << "\n";

  TreeNode *root2 = tree2();
  b = s.isSameTree(root1, root2);
  std::cout << "is not same tree " << (b ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
