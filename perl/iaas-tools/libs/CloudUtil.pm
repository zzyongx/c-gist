package CloudUtil;

use strict;
use warnings;

use Time::Piece;

our $LoopCallInterval = 30;
our $LoopCallTimeOut  = 900;

sub iso8601
{
    my $t = localtime;
    return $t->ymd(). "T". $t->hms('%3A'). "Z";
}

sub makevsn
{
    my $t = localtime;
    return substr($t->ymd("."). ".". $t->hms("."), 2, -3);
}

sub makeimagenam 
{
    my ($os, $vsn, $arch) = @_;
    return "$os.$vsn.$arch";
}

sub loop_call
{
    my $fun = shift;
    my $timer = shift || $LoopCallTimeOut;

    while (1) {
        if (&$fun()) {
            return 1;
        } elsif ($timer > 0) {
            sleep($LoopCallInterval);
            $timer -= $LoopCallInterval;
        } else {
            return 0;
        }
    }

    return 0;
}

1;
