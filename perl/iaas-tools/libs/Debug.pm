package Debug;

our $On = 0;

sub print
{
    CORE::print @_, "\n" if ($On > 0);
}

sub verbose
{
    CORE::print @_, "\n" if ($On > 1);
}

1;
