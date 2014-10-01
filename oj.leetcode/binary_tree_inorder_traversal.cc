#include <vector>
#include <iostream>

struct TreeNode {
  int val;
  TreeNode *left;
  TreeNode *right;
  TreeNode(int x) : val(x), left(NULL), right(NULL) {}
};

class Solution {
public:
  std::vector<int> inorderTraversal(TreeNode *root) {
    std::vector<int> v;
    if (!root) return v;
    
    if (root->left) {
      std::vector<int> t = inorderTraversal(root->left);
      v.insert(v.end(), t.begin(), t.end());
    }
    
    v.push_back(root->val);

    if (root->right) {
      std::vector<int> t = inorderTraversal(root->right);
      v.insert(v.end(), t.begin(), t.end());
    }
    return v;
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

  n4->right = n5;

  return n0;
}

int main()
{
  Solution s;
  std::vector<int> v;

  TreeNode *root1 = tree1();
  v = s.inorderTraversal(root1);
  int A1[] = {3, 1, 4, 5, 0, 2};
  std::vector<int> V1(A1, A1+sizeof(A1)/sizeof(int));
  std::cout << "inorder traversal " << (V1 == v ? "TRUE" : "FALSE") << "\n";

  return 0;  
}
