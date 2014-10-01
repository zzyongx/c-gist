#include <iostream>

struct TreeLinkNode {
  int val;
  TreeLinkNode *left, *right, *next;
  TreeLinkNode(int x) : val(x), left(NULL), right(NULL), next(NULL) {}
};

class Solution {
publ ic:
  void connect(TreeLinkNode *root, TreeLinkNode *ptr) {
    if (root && root->left && root->right) {
      root->left->next = root->right;
      root->right->next = ptr;
      if (root->right->left) {
        connect(root->left, root->right->left);
        connect(root->right, ptr);
      }
    }
  }
  
  void connect(TreeLinkNode *root) {
    if (!root) return;
    connect(root, NULL);
  }
};

int main()
{
  Solution s;

  TreeLinkNode *n0 = new TreeLinkNode(0);
  TreeLinkNode *n1 = new TreeLinkNode(1);
  TreeLinkNode *n2 = new TreeLinkNode(2);
  TreeLinkNode *n3 = new TreeLinkNode(3);
  TreeLinkNode *n4 = new TreeLinkNode(4);
  TreeLinkNode *n5 = new TreeLinkNode(5);
  TreeLinkNode *n6 = new TreeLinkNode(6);

  n0->left = n1;
  n0->right = n2;

  n1->left = n3;
  n1->right = n4;
  n2->left = n5;
  n2->right = n6;

  s.connect(n0);

  if (n0->next == NULL &&
      n1->next == n2 && n2->next == NULL &&
      n3->next == n4 && n4->next == n5 && n5->next == n6 && n6->next == NULL) {
    std::cout << "connect TRUE\n";
  } else {
    std::cout << "connect FALSE\n";
  }

  return 0;
}  
